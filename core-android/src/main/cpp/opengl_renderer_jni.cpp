#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <EGL/eglplatform.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <jni.h>

#include <cassert>
#include <iomanip>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

namespace {
    auto constexpr LOG_TAG = "OpenGLRendererJni";

    std::string GLErrorString(GLenum error) {
        switch (error) {
            case GL_NO_ERROR:
                return "GL_NO_ERROR";
            case GL_INVALID_ENUM:
                return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE:
                return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION:
                return "GL_INVALID_OPERATION";
            case GL_STACK_OVERFLOW_KHR:
                return "GL_STACK_OVERFLOW";
            case GL_STACK_UNDERFLOW_KHR:
                return "GL_STACK_UNDERFLOW";
            case GL_OUT_OF_MEMORY:
                return "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: {
                std::ostringstream oss;
                oss << "<Unknown GL Error 0x" << std::setfill('0') <<
                    std::setw(4) << std::right << std::hex << error << ">";
                return oss.str();
            }
        }
    }

    std::string EGLErrorString(EGLenum error) {
        switch (error) {
            case EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default: {
                std::ostringstream oss;
                oss << "<Unknown EGL Error 0x" << std::setfill('0') <<
                    std::setw(4) << std::right << std::hex << error << ">";
                return oss.str();
            }
        }
    }
}

#ifdef NDEBUG
#define CHECK_GL(gl_func) [&]() { return gl_func; }()
#else
namespace {
    class CheckGlErrorOnExit {
    public:
        explicit CheckGlErrorOnExit(std::string glFunStr, unsigned int lineNum) :
                mGlFunStr(std::move(glFunStr)),
                mLineNum(lineNum) {}

        ~CheckGlErrorOnExit() {
            GLenum err = glGetError();
            if (err != GL_NO_ERROR) {
                __android_log_assert(nullptr, LOG_TAG, "OpenGL Error: %s at %s [%s:%d]",
                                     GLErrorString(err).c_str(), mGlFunStr.c_str(), __FILE__,
                                     mLineNum);
            }
        }

        CheckGlErrorOnExit(const CheckGlErrorOnExit &) = delete;

        CheckGlErrorOnExit &operator=(const CheckGlErrorOnExit &) = delete;

    private:
        std::string mGlFunStr;
        unsigned int mLineNum;
    };  // class CheckGlErrorOnExit
}   // namespace
#define CHECK_GL(glFunc)                                                    \
  [&]() {                                                                   \
    auto assertOnExit = CheckGlErrorOnExit(#glFunc, __LINE__);              \
    return glFunc;                                                          \
  }()
#endif

namespace {
    constexpr char VERTEX_SHADER_SRC_NO_BLUR[] = R"SRC(#version 310 es
precision mediump float;
precision mediump int;

uniform mat4 vertTransform;

in vec4 position;
out vec2 texCoord;

void main() {
    texCoord = ((vertTransform * vec4(position.xy, 0., 1.)).xy + vec2(1.)) * 0.5;
    gl_Position = position;
}
)SRC";

    constexpr char VERTEX_SHADER_SRC_TRANSFORM[] = R"SRC(#version 310 es
precision mediump float;
precision mediump int;

uniform mat4 vertTransform;

in vec4 position;
out vec2 texCoord;

void main() {
    texCoord = ((vertTransform * vec4(position.xy, 0., 1.)).xy + vec2(1.)) * 0.5;
    gl_Position = position;
}
)SRC";

    constexpr char VERTEX_SHADER_SRC_NO_TRANSFORM[] = R"SRC(#version 310 es
precision mediump float;
precision mediump int;

in vec4 position;
out vec2 texCoord;

void main() {
    texCoord = (position.xy + vec2(1.)) * 0.5;
    gl_Position = position;
}
)SRC";

    constexpr char FRAGMENT_SHADER_SRC_NO_BLUR[] = R"SRC(#version 310 es
#extension GL_OES_EGL_image_external : require
precision mediump float;
precision mediump int;

uniform samplerExternalOES sampler;
uniform mat4 texTransform;
uniform float width;
uniform float height;
uniform int x;
uniform int y;
uniform float cornerRadius;

in vec2 texCoord;
out vec4 fragColor;

float udRoundBox(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b + r, 0.)) - r;
}

float computeBox() {
    vec2 res = vec2(width, height);
    vec2 coord = vec2(gl_FragCoord.x - float(x), gl_FragCoord.y - float(y));
    return udRoundBox(2. * coord - res, res, cornerRadius);
}

void main() {
    vec2 transTexCoord = (texTransform * vec4(texCoord, 0., 1.)).xy;
    vec4 texColor = texture(sampler, transTexCoord);
    if (cornerRadius > 0.) {
        float box = computeBox();
        vec3 color = mix(texColor.rgb, vec3(0.), smoothstep(0., 1., box));
        if (box > 1.) {
            discard;
        } else {
            fragColor = texColor;
        }
    } else {
        fragColor = texColor;
    }
}
)SRC";

    constexpr char FRAGMENT_SHADER_SRC_V_OES[] = R"SRC(#version 310 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
precision mediump int;

uniform samplerExternalOES sampler;
uniform mat4 texTransform;
uniform float height;
uniform float lod;
uniform float minLod;
uniform vec3 contrastingColor;
uniform float contrastingColorMix;

in vec2 texCoord;
out vec4 fragColor;

const float sigma = 3.;
const float r = sigma * 2.;
const float invTwoSigmaSqr = 1. / (2. * sigma * sigma);

vec4 gaussBlur( samplerExternalOES tex, vec2 uv, vec2 d, float l )
{
    vec4 c = texture(tex, uv, l);
    for (float i = 1.; i < r; ++i) {
        c += (
            texture(tex, uv + d * i, l) +
            texture(tex, uv - d * i, l)
        ) * exp(- i * i * invTwoSigmaSqr);
    }
    return c / c.a;
}

void main() {
    vec2 transTexCoord = (texTransform * vec4(texCoord, 0., 1.)).xy;
    if (lod > minLod) {
        vec4 blurred = gaussBlur(sampler, transTexCoord, vec2(0., exp2(lod) / height), lod);
        if (contrastingColor != vec3(-1.)) {
            fragColor = vec4(mix(vec3(blurred.rgb), contrastingColor, contrastingColorMix), blurred.a);
        } else {
            fragColor = blurred;
        }
    } else {
        fragColor = texture(sampler, transTexCoord);
    }
}
)SRC";

    constexpr char FRAGMENT_SHADER_SRC_V_2D[] = R"SRC(#version 310 es
precision mediump float;
precision mediump int;

uniform sampler2D sampler;
uniform float height;
uniform float lod;
uniform float minLod;
uniform vec3 contrastingColor;
uniform float contrastingColorMix;

in vec2 texCoord;
out vec4 fragColor;

const float sigma = 3.;
const float r = sigma * 2.;
const float invTwoSigmaSqr = 1. / (2. * sigma * sigma);

vec4 gaussBlur( sampler2D tex, vec2 uv, vec2 d, float l )
{
    vec4 c = texture(tex, uv, l);
    for (float i = 1.; i < r; ++i) {
        c += (
            texture(tex, uv + d * i, l) +
            texture(tex, uv - d * i, l)
        ) * exp(- i * i * invTwoSigmaSqr);
    }
    return c / c.a;
}

void main() {
    if (lod > minLod) {
        vec4 blurred = gaussBlur(sampler, texCoord, vec2(0., exp2(lod) / height), lod);
        if (contrastingColor != vec3(-1.)) {
            fragColor = vec4(mix(vec3(blurred.rgb), contrastingColor, contrastingColorMix), blurred.a);
        } else {
            fragColor = blurred;
        }
    } else {
        fragColor = texture(sampler, texCoord);
    }
}
)SRC";

    constexpr char FRAGMENT_SHADER_SRC_H[] = R"SRC(#version 310 es
precision mediump float;
precision mediump int;

uniform sampler2D sampler;
uniform float width;
uniform float lod;
uniform float minLod;
uniform vec3 contrastingColor;
uniform float contrastingColorMix;

in vec2 texCoord;
out vec4 fragColor;

const float sigma = 3.;
const float r = sigma * 2.;
const float invTwoSigmaSqr = 1. / (2. * sigma * sigma);

vec4 gaussBlur( sampler2D tex, vec2 uv, vec2 d, float l )
{
    vec4 c = texture(tex, uv, l);
    for (float i = 1.; i < r; ++i) {
        c += (
            texture(tex, uv + d * i, l) +
            texture(tex, uv - d * i, l)
        ) * exp(- i * i * invTwoSigmaSqr);
    }
    return c / c.a;
}

void main() {
    if (lod > minLod) {
        vec4 blurred = gaussBlur(sampler, texCoord, vec2(exp2(lod) / width, 0.), lod);
        if (contrastingColor != vec3(-1.)) {
            fragColor = vec4(mix(vec3(blurred.rgb), contrastingColor, contrastingColorMix), blurred.a);
        } else {
            fragColor = blurred;
        }
    } else {
        fragColor = texture(sampler, texCoord);
    }
}
)SRC";

    struct NativeContext {
        EGLDisplay display;
        EGLConfig config;
        EGLContext context;
        std::pair<ANativeWindow *, EGLSurface> windowSurface;
        EGLSurface bufferSurface;

        GLuint programNoBlur = -1;
        GLint positionHandleNoBlur = -1;
        GLint samplerHandleNoBlur = -1;
        GLint vertTransformHandleNoBlur = -1;
        GLint texTransformHandleNoBlur = -1;
        GLint widthHandleNoBlur = -1;
        GLint heightHandleNoBlur = -1;
        GLint xHandleNoBlur = -1;
        GLint yHandleNoBlur = -1;
        GLint cornerRadiusHandleNoBlur = -1;

        GLuint programVOES = -1;
        GLint positionHandleVOES = -1;
        GLint samplerHandleVOES = -1;
        GLint vertTransformHandleVOES = -1;
        GLint texTransformHandleVOES = -1;
        GLint heightHandleVOES = -1;
        GLint lodHandleVOES = -1;
        GLint minLodHandleVOES = -1;
        GLint contrastingColorHandleVOES = -1;
        GLint contrastingColorMixHandleVOES = -1;

        GLuint programH = -1;
        GLint positionHandleH = -1;
        GLint samplerHandleH = -1;
        GLint widthHandleH = -1;
        GLint lodHandleH = -1;
        GLint minLodHandleH = -1;
        GLint contrastingColorHandleH = -1;
        GLint contrastingColorMixHandleH = -1;

        GLuint programV2D = -1;
        GLint positionHandleV2D = -1;
        GLint samplerHandleV2D = -1;
        GLint heightHandleV2D = -1;
        GLint lodHandleV2D = -1;
        GLint minLodHandleV2D = -1;
        GLint contrastingColorHandleV2D = -1;
        GLint contrastingColorMixHandleV2D = -1;

        GLuint inputTextureId = -1;
        GLuint pass1TextureId = -1;
        GLuint fbo1Id = -1;
        GLuint pass2TextureId = -1;
        GLuint fbo2Id = -1;
        GLuint pass3TextureId = -1;
        GLuint fbo3Id = -1;
        GLuint pass4TextureId = -1;
        GLuint fbo4Id = -1;
        GLuint pass5TextureId = -1;
        GLuint fbo5Id = -1;
        GLuint pass6TextureId = -1;
        GLuint fbo6Id = -1;
        GLuint pass7TextureId = -1;
        GLuint fbo7Id = -1;

        GLboolean blurEnabled = GL_FALSE;
        GLfloat lod = MIN_LOD;
        GLint currentBlurAnimationFrame = -1;

        GLfloat contrastingColorMix = MAX_CONTRASTING_COLOR_MIX;

        GLint currentContrastingColorAnimationFrame
                = NativeContext::CONTRASTING_COLOR_ANIMATION_FRAMES;
        GLfloat targetContrastingRed = -1.f;
        GLfloat targetContrastingGreen = -1.f;
        GLfloat targetContrastingBlue = -1.f;
        GLfloat contrastingRed = -1.f;
        GLfloat contrastingGreen = -1.f;
        GLfloat contrastingBlue = -1.f;

        GLint vertexComponents = 2;
        GLenum vertexType = GL_FLOAT;
        GLboolean normalized = GL_FALSE;
        GLsizei vertexStride = 0;

        GLsizei numMatrices = 1;
        GLboolean transpose = GL_FALSE;

        // We use a single triangle with the viewport inscribed within for our
        // VERTICES. This could also be done with a quad or two triangles.
        //                          ^
        //                          |
        //                       (-1,3)
        //                          +_
        //                          | \_
        //                          |   \_
        //                       (-1,1)   \(1,1)
        //                          +-------+_
        //                          |       | \_
        //                          |   +   |   \_
        //                          |       |     \_
        //                          +-------+-------+-->
        //                       (-1,-1)  (1,-1)  (3,-1)
        static constexpr GLfloat VERTICES[] = {-1.f, -1.f, 3.f, -1.f, -1.f, 3.f};

        static constexpr GLfloat MAX_LOD = 2.f;
        static constexpr GLfloat MIN_LOD = -2.f;
        static constexpr GLint BLUR_ANIMATION_FRAMES = 18;
        static constexpr GLfloat LOD_INCREMENT = (NativeContext::MAX_LOD - NativeContext::MIN_LOD) /
                                                 (GLfloat) NativeContext::BLUR_ANIMATION_FRAMES;

        static constexpr GLfloat MAX_CONTRASTING_COLOR_MIX = .05f;
        static constexpr GLfloat MIN_CONTRASTING_COLOR_MIX = 0.f;
        static constexpr GLfloat CONTRASTING_COLOR_MIX_INCREMENT =
                (NativeContext::MAX_CONTRASTING_COLOR_MIX -
                 NativeContext::MIN_CONTRASTING_COLOR_MIX) /
                (GLfloat) NativeContext::BLUR_ANIMATION_FRAMES;

        static constexpr GLint CONTRASTING_COLOR_ANIMATION_FRAMES = 60;

        NativeContext(EGLDisplay display,
                      EGLConfig config,
                      EGLContext context,
                      ANativeWindow *window,
                      EGLSurface surface,
                      EGLSurface pbufferSurface)
                : display(display),
                  config(config),
                  context(context),
                  windowSurface(std::make_pair(window, surface)),
                  bufferSurface(pbufferSurface) {}

    private:
        void PrepareDrawNoBlur(const GLfloat *vertTransformArray,
                               const GLfloat *texTransformArray,
                               GLfloat width,
                               GLfloat height,
                               GLint x,
                               GLint y,
                               GLfloat cornerRadius) const {
            CHECK_GL(glVertexAttribPointer(positionHandleNoBlur,
                                           vertexComponents, vertexType, normalized,
                                           vertexStride, VERTICES));
            CHECK_GL(glEnableVertexAttribArray(positionHandleNoBlur));
            CHECK_GL(glUseProgram(programNoBlur));
            CHECK_GL(glUniformMatrix4fv(vertTransformHandleNoBlur, numMatrices, transpose,
                                        vertTransformArray));
            CHECK_GL(glUniform1i(samplerHandleNoBlur, 0));
            CHECK_GL(glUniformMatrix4fv(texTransformHandleNoBlur, numMatrices,
                                        transpose, texTransformArray));
            CHECK_GL(glUniform1f(widthHandleNoBlur, width));
            CHECK_GL(glUniform1f(heightHandleNoBlur, height));
            CHECK_GL(glUniform1i(xHandleNoBlur, x));
            CHECK_GL(glUniform1i(yHandleNoBlur, y));
            CHECK_GL(glUniform1f(cornerRadiusHandleNoBlur, cornerRadius));
            CHECK_GL(glBindTexture(GL_TEXTURE_EXTERNAL_OES, inputTextureId));
        }

        void PrepareDrawVOES(const GLfloat *vertTransformArray,
                             const GLfloat *texTransformArray,
                             GLfloat height,
                             bool withMaxLod,
                             bool mixContrastingColor) const {
            CHECK_GL(glVertexAttribPointer(positionHandleVOES,
                                           vertexComponents, vertexType, normalized,
                                           vertexStride, VERTICES));
            CHECK_GL(glEnableVertexAttribArray(positionHandleVOES));
            CHECK_GL(glUseProgram(programVOES));
            CHECK_GL(glUniformMatrix4fv(
                    vertTransformHandleVOES, numMatrices, transpose,
                    vertTransformArray));
            CHECK_GL(glUniform1i(samplerHandleVOES, 0));
            CHECK_GL(glUniformMatrix4fv(texTransformHandleVOES, numMatrices,
                                        transpose, texTransformArray));
            CHECK_GL(glUniform1f(heightHandleVOES, height));
            CHECK_GL(glUniform1f(lodHandleVOES, withMaxLod ? NativeContext::MAX_LOD : lod));
            CHECK_GL(glUniform1f(minLodHandleVOES, NativeContext::MIN_LOD));
            if (mixContrastingColor) {
                CHECK_GL(glUniform3f(contrastingColorHandleVOES,
                                     contrastingRed, contrastingGreen, contrastingBlue));
                CHECK_GL(glUniform1f(contrastingColorMixHandleVOES, contrastingColorMix));
            } else {
                CHECK_GL(glUniform3f(contrastingColorHandleVOES, -1.f, -1.f, -1.f));
                CHECK_GL(glUniform1f(contrastingColorMixHandleVOES, 0.f));
            }
        }

        void PrepareDrawV2D(GLfloat height, bool withMaxLod, bool mixContrastingColor) const {
            CHECK_GL(glVertexAttribPointer(positionHandleV2D,
                                           vertexComponents, vertexType, normalized,
                                           vertexStride, VERTICES));
            CHECK_GL(glEnableVertexAttribArray(positionHandleV2D));
            CHECK_GL(glUseProgram(programV2D));
            CHECK_GL(glUniform1i(samplerHandleV2D, 0));
            CHECK_GL(glUniform1f(heightHandleV2D, height));
            CHECK_GL(glUniform1f(lodHandleV2D, withMaxLod ? NativeContext::MAX_LOD : lod));
            CHECK_GL(glUniform1f(minLodHandleV2D, NativeContext::MIN_LOD));
            if (mixContrastingColor) {
                CHECK_GL(glUniform3f(contrastingColorHandleV2D,
                                     contrastingRed, contrastingGreen, contrastingBlue));
                CHECK_GL(glUniform1f(contrastingColorMixHandleV2D, contrastingColorMix));
            } else {
                CHECK_GL(glUniform3f(contrastingColorHandleV2D, -1.f, -1.f, -1.f));
                CHECK_GL(glUniform1f(contrastingColorMixHandleV2D, 0.f));
            }
        }

        void PrepareDrawH(GLfloat width, bool withMaxLod, bool mixContrastingColor) const {
            CHECK_GL(glVertexAttribPointer(positionHandleH,
                                           vertexComponents, vertexType, normalized,
                                           vertexStride, VERTICES));
            CHECK_GL(glEnableVertexAttribArray(positionHandleH));
            CHECK_GL(glUseProgram(programH));
            CHECK_GL(glUniform1i(samplerHandleH, 0));
            CHECK_GL(glUniform1f(widthHandleH, width));
            CHECK_GL(glUniform1f(lodHandleH, withMaxLod ? NativeContext::MAX_LOD : lod));
            CHECK_GL(glUniform1f(minLodHandleH, NativeContext::MIN_LOD));
            if (mixContrastingColor) {
                CHECK_GL(glUniform3f(contrastingColorHandleH,
                                     contrastingRed, contrastingGreen, contrastingBlue));
                CHECK_GL(glUniform1f(contrastingColorMixHandleH, contrastingColorMix));
            } else {
                CHECK_GL(glUniform3f(contrastingColorHandleH, -1.f, -1.f, -1.f));
                CHECK_GL(glUniform1f(contrastingColorMixHandleH, 0.f));
            }
        }

        static void BindAndDraw(GLuint fboId, GLuint textureId, GLenum texTarget = GL_TEXTURE_2D) {
            CHECK_GL(glBindFramebuffer(GL_FRAMEBUFFER, fboId));
            CHECK_GL(glBindTexture(texTarget, textureId));
            CHECK_GL(glDrawArrays(GL_TRIANGLES, 0, 3));
        }

        static void PrepareStencilForDrawingRects() {
            CHECK_GL(glEnable(GL_STENCIL_TEST));
            CHECK_GL(glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE));
            CHECK_GL(glStencilFunc(GL_ALWAYS, 1, 0xFF));
            CHECK_GL(glStencilMask(0xFF));
        }

        void DrawBlurredRects(const GLfloat *vertTransformArray,
                              const GLfloat *texTransformArray,
                              GLfloat width,
                              GLfloat height) const {
            CHECK_GL(glStencilFunc(GL_EQUAL, 1, 0xFF));
            CHECK_GL(glScissor(0, 0, width, height));
            DrawBlur(vertTransformArray, texTransformArray, width, height, true, true);
        }

        void DrawScissoredRectsInStencil(const GLfloat *vertTransformArray,
                                         const GLfloat *texTransformArray,
                                         GLfloat *rectsCoordinates,
                                         GLuint rectsCount,
                                         GLfloat width,
                                         GLfloat height) const {
            GLfloat *rectCoordinate = rectsCoordinates;
            for (GLuint i = 0; i < rectsCount; ++i) {
                auto rectLeftX = *rectCoordinate;
                ++rectCoordinate;
                auto rectBottomY = height - *rectCoordinate;
                ++rectCoordinate;
                auto rectWidth = *rectCoordinate;
                ++rectCoordinate;
                auto rectHeight = *rectCoordinate;
                ++rectCoordinate;
                auto cornerRadius = *rectCoordinate;
                ++rectCoordinate;
                CHECK_GL(glScissor(rectLeftX, rectBottomY, rectWidth, rectHeight));
                DrawNoBlur(vertTransformArray, texTransformArray,
                           rectWidth, rectHeight,
                           (GLint) rectLeftX, (GLint) rectBottomY,
                           cornerRadius);
            }
        }

    public:
        [[nodiscard]] GLboolean IsAnimatingContrastingColor() const {
            return currentContrastingColorAnimationFrame
                   < NativeContext::CONTRASTING_COLOR_ANIMATION_FRAMES;
        }

        void AnimateContrastingColor() {
            auto fraction = (GLfloat) (currentContrastingColorAnimationFrame + 1) /
                            (GLfloat) NativeContext::CONTRASTING_COLOR_ANIMATION_FRAMES;
            contrastingRed = (targetContrastingRed - contrastingRed) * fraction + contrastingRed;
            contrastingGreen =
                    (targetContrastingGreen - contrastingGreen) * fraction + contrastingGreen;
            contrastingBlue =
                    (targetContrastingBlue - contrastingBlue) * fraction + contrastingBlue;
            ++currentContrastingColorAnimationFrame;
        }

        [[nodiscard]] GLboolean IsAnimatingLod() const {
            return currentBlurAnimationFrame > -1 &&
                   currentBlurAnimationFrame < NativeContext::BLUR_ANIMATION_FRAMES;
        }

        void AnimateLod() {
            if (blurEnabled) {
                if (lod < NativeContext::MAX_LOD) {
                    lod += NativeContext::LOD_INCREMENT;
                }
                if (contrastingColorMix > NativeContext::MIN_CONTRASTING_COLOR_MIX) {
                    contrastingColorMix -= CONTRASTING_COLOR_MIX_INCREMENT;
                }
                ++currentBlurAnimationFrame;
            } else {
                if (lod > NativeContext::MIN_LOD) {
                    lod -= NativeContext::LOD_INCREMENT;
                }
                if (contrastingColorMix <
                    NativeContext::MAX_CONTRASTING_COLOR_MIX) {
                    contrastingColorMix += NativeContext::CONTRASTING_COLOR_MIX_INCREMENT;
                }
                --currentBlurAnimationFrame;
            }
        }

        void DrawNoBlur(const GLfloat *vertTransformArray,
                        const GLfloat *texTransformArray,
                        GLfloat width,
                        GLfloat height,
                        GLint x,
                        GLint y,
                        GLfloat cornerRadius) const {
            PrepareDrawNoBlur(vertTransformArray, texTransformArray,
                              width, height,
                              x, y,
                              cornerRadius);
            CHECK_GL(glViewport(x, y, width, height));
            CHECK_GL(glDrawArrays(GL_TRIANGLES, 0, 3));
        }

        void DrawBlur(const GLfloat *vertTransformArray,
                      const GLfloat *texTransformArray,
                      GLfloat width,
                      GLfloat height,
                      bool withMaxLod = false,
                      bool mixContrastingColor = false) const {
            PrepareDrawVOES(vertTransformArray, texTransformArray, height / 2.f,
                            withMaxLod, mixContrastingColor);
            CHECK_GL(glViewport(0, 0, width / 2.f, height / 2.f));
            BindAndDraw(fbo1Id, inputTextureId, GL_TEXTURE_EXTERNAL_OES);

            PrepareDrawH(width / 2.f, withMaxLod, mixContrastingColor);
            BindAndDraw(fbo2Id, pass1TextureId);

            PrepareDrawV2D(height / 4.f, withMaxLod, mixContrastingColor);
            CHECK_GL(glViewport(0, 0, width / 4.f, height / 4.f));
            BindAndDraw(fbo3Id, pass2TextureId);

            PrepareDrawH(width / 4.f, withMaxLod, mixContrastingColor);
            BindAndDraw(fbo4Id, pass3TextureId);

            PrepareDrawV2D(height / 2.f, withMaxLod, mixContrastingColor);
            CHECK_GL(glViewport(0, 0, width / 2.f, height / 2.f));
            BindAndDraw(fbo5Id, pass4TextureId);

            PrepareDrawH(width / 2.f, withMaxLod, mixContrastingColor);
            BindAndDraw(fbo6Id, pass5TextureId);

            PrepareDrawV2D(height, withMaxLod, mixContrastingColor);
            CHECK_GL(glViewport(0, 0, width, height));
            BindAndDraw(fbo7Id, pass6TextureId);

            PrepareDrawH(width, withMaxLod, mixContrastingColor);
            BindAndDraw(0, pass7TextureId);
        }

        void DrawBlurredRects(const GLfloat *vertTransformArray,
                              const GLfloat *texTransformArray,
                              GLfloat *rectsCoordinates,
                              GLuint rectsCount,
                              GLfloat width,
                              GLfloat height) {
            PrepareStencilForDrawingRects();
            DrawScissoredRectsInStencil(vertTransformArray, texTransformArray,
                                        rectsCoordinates, rectsCount,
                                        width, height);
            DrawBlurredRects(vertTransformArray, texTransformArray, width, height);
        }
    };

    void InitFrameBuffer(GLuint *textureId, GLuint *fboId, GLsizei width, GLsizei height) {
        CHECK_GL(glGenTextures(1, textureId));
        CHECK_GL(glBindTexture(GL_TEXTURE_2D, *textureId));
        CHECK_GL(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR));
        CHECK_GL(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE));
        CHECK_GL(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE));
        CHECK_GL(glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE,
                              nullptr));
        CHECK_GL(glBindTexture(GL_TEXTURE_2D, 0));

        CHECK_GL(glGenFramebuffers(1, fboId));
        CHECK_GL(glBindFramebuffer(GL_FRAMEBUFFER, *fboId));
        CHECK_GL(glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                                        *textureId, 0));
        CHECK_GL(glBindFramebuffer(GL_FRAMEBUFFER, 0));
    }

    const char *ShaderTypeString(GLenum shaderType) {
        switch (shaderType) {
            case GL_VERTEX_SHADER:
                return "GL_VERTEX_SHADER";
            case GL_FRAGMENT_SHADER:
                return "GL_FRAGMENT_SHADER";
            default:
                return "<Unknown shader type>";
        }
    }

    // Returns a handle to the shader
    GLuint CompileShader(GLenum shaderType, const char *shaderSrc) {
        GLuint shader = CHECK_GL(glCreateShader(shaderType));
        assert(shader);
        CHECK_GL(glShaderSource(shader, 1, &shaderSrc, /*length=*/nullptr));
        CHECK_GL(glCompileShader(shader));
        GLint compileStatus = 0;
        CHECK_GL(glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus));
        if (!compileStatus) {
            GLint logLength = 0;
            CHECK_GL(glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLength));
            std::vector<char> logBuffer(logLength);
            if (logLength > 0) {
                CHECK_GL(glGetShaderInfoLog(shader, logLength, /*length=*/nullptr,
                                            &logBuffer[0]));
            }
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                "Unable to compile %s shader:\n %s.",
                                ShaderTypeString(shaderType),
                                logLength > 0 ? &logBuffer[0] : "(unknown error)");
            CHECK_GL(glDeleteShader(shader));
            shader = 0;
        }
        assert(shader);
        return shader;
    }

    // Returns a handle to the output program
    GLuint CreateGlProgram(const char *vertexShaderSrc, const char *fragmentShaderSrc) {
        GLuint vertexShader = CompileShader(GL_VERTEX_SHADER, vertexShaderSrc);
        assert(vertexShader);

        GLuint fragmentShader = CompileShader(GL_FRAGMENT_SHADER, fragmentShaderSrc);
        assert(fragmentShader);

        GLuint program = CHECK_GL(glCreateProgram());
        assert(program);
        CHECK_GL(glAttachShader(program, vertexShader));
        CHECK_GL(glAttachShader(program, fragmentShader));
        CHECK_GL(glLinkProgram(program));
        GLint linkStatus = 0;
        CHECK_GL(glGetProgramiv(program, GL_LINK_STATUS, &linkStatus));
        if (!linkStatus) {
            GLint logLength = 0;
            CHECK_GL(glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLength));
            std::vector<char> logBuffer(logLength);
            if (logLength > 0) {
                CHECK_GL(glGetProgramInfoLog(program, logLength, /*length=*/nullptr,
                                             &logBuffer[0]));
            }
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                "Unable to link program:\n %s.",
                                logLength > 0 ? &logBuffer[0] : "(unknown error)");
            CHECK_GL(glDeleteProgram(program));
            program = 0;
        }
        assert(program);
        return program;
    }

    void DestroySurface(NativeContext *nativeContext) {
        if (nativeContext->windowSurface.first) {
            eglMakeCurrent(nativeContext->display, nativeContext->bufferSurface,
                           nativeContext->bufferSurface, nativeContext->context);
            eglDestroySurface(nativeContext->display,
                              nativeContext->windowSurface.second);
            nativeContext->windowSurface.second = nullptr;
            ANativeWindow_release(nativeContext->windowSurface.first);
            nativeContext->windowSurface.first = nullptr;
        }
    }

    void ThrowException(JNIEnv *env, const char *exceptionName, const char *msg) {
        jclass exClass = env->FindClass(exceptionName);
        assert(exClass != nullptr);

        [[maybe_unused]] jint throwSuccess = env->ThrowNew(exClass, msg);
        assert(throwSuccess == JNI_OK);
    }
}  // namespace

extern "C" {
JNIEXPORT jlong JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_initContext(
        JNIEnv *env, jobject clazz) {
    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    assert(eglDisplay != EGL_NO_DISPLAY);

    EGLint majorVer;
    EGLint minorVer;
    EGLBoolean initSuccess = eglInitialize(eglDisplay, &majorVer, &minorVer);
    if (initSuccess != EGL_TRUE) {
        ThrowException(env, "java/lang/RuntimeException",
                       "EGL Error: eglInitialize failed.");
        return 0;
    }

    // Print debug EGL information
    const char *eglVendorString = eglQueryString(eglDisplay, EGL_VENDOR);
    const char *eglVersionString = eglQueryString(eglDisplay, EGL_VERSION);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "EGL Initialized [Vendor: %s, Version: %s]",
                        eglVendorString == nullptr ? "Unknown" : eglVendorString,
                        eglVersionString == nullptr
                        ? "Unknown" : eglVersionString);

    EGLint configAttribs[] = {EGL_RENDERABLE_TYPE,
                              EGL_OPENGL_ES3_BIT,
                              EGL_SURFACE_TYPE,
                              EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
                              EGL_STENCIL_SIZE, 8,
                              EGL_RECORDABLE_ANDROID,
                              EGL_TRUE,
                              EGL_NONE};
    EGLConfig config;
    EGLint numConfigs;
    EGLint configSize = 1;
    EGLBoolean chooseConfigSuccess =
            eglChooseConfig(eglDisplay, configAttribs, &config, configSize, &numConfigs);
    if (chooseConfigSuccess != EGL_TRUE) {
        ThrowException(env, "java/lang/IllegalArgumentException",
                       "EGL Error: eglChooseConfig failed. ");
        return 0;
    }
    assert(numConfigs > 0);

    int contextAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    EGLContext eglContext = eglCreateContext(
            eglDisplay, config, EGL_NO_CONTEXT, static_cast<EGLint *>(contextAttribs));
    assert(eglContext != EGL_NO_CONTEXT);

    // Create 1x1 pixmap to use as a surface until one is set.
    int pbufferAttribs[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
    EGLSurface eglPbuffer =
            eglCreatePbufferSurface(eglDisplay, config, pbufferAttribs);
    assert(eglPbuffer != EGL_NO_SURFACE);

    eglMakeCurrent(eglDisplay, eglPbuffer, eglPbuffer, eglContext);

    //Print debug OpenGL information
    const GLubyte *glVendorString = CHECK_GL(glGetString(GL_VENDOR));
    const GLubyte *glVersionString = CHECK_GL(glGetString(GL_VERSION));
    const GLubyte *glslVersionString = CHECK_GL(glGetString(GL_SHADING_LANGUAGE_VERSION));
    const GLubyte *glRendererString = CHECK_GL(glGetString(GL_RENDERER));
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "OpenGL Initialized [Vendor: %s, Version: %s,"
                                                    " GLSL Version: %s, Renderer: %s]",
                        glVendorString == nullptr ? "Unknown" : (const char *) glVendorString,
                        glVersionString == nullptr ? "Unknown" : (const char *) glVersionString,
                        glslVersionString == nullptr ? "Unknown" : (const char *) glslVersionString,
                        glRendererString == nullptr ? "Unknown" : (const char *) glRendererString);

    auto *nativeContext =
            new NativeContext(eglDisplay, config, eglContext, /*window=*/nullptr,
                    /*surface=*/nullptr, eglPbuffer);

    nativeContext->programNoBlur = CreateGlProgram(VERTEX_SHADER_SRC_NO_BLUR,
                                                   FRAGMENT_SHADER_SRC_NO_BLUR);
    assert(nativeContext->programNoBlur);
    nativeContext->positionHandleNoBlur =
            CHECK_GL(glGetAttribLocation(nativeContext->programNoBlur, "position"));
    assert(nativeContext->positionHandleNoBlur != -1);
    nativeContext->samplerHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "sampler"));
    assert(nativeContext->samplerHandleNoBlur != -1);
    nativeContext->vertTransformHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "vertTransform"));
    assert(nativeContext->vertTransformHandleNoBlur != -1);
    nativeContext->texTransformHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "texTransform"));
    assert(nativeContext->texTransformHandleNoBlur != -1);
    nativeContext->widthHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "width"));
    assert(nativeContext->widthHandleNoBlur != -1);
    nativeContext->heightHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "height"));
    assert(nativeContext->heightHandleNoBlur != -1);
    nativeContext->xHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "x"));
    assert(nativeContext->xHandleNoBlur != -1);
    nativeContext->yHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "y"));
    assert(nativeContext->yHandleNoBlur != -1);
    nativeContext->cornerRadiusHandleNoBlur =
            CHECK_GL(glGetUniformLocation(nativeContext->programNoBlur, "cornerRadius"));
    assert(nativeContext->cornerRadiusHandleNoBlur != -1);

    nativeContext->programVOES = CreateGlProgram(VERTEX_SHADER_SRC_TRANSFORM,
                                                 FRAGMENT_SHADER_SRC_V_OES);
    assert(nativeContext->programVOES);
    nativeContext->positionHandleVOES =
            CHECK_GL(glGetAttribLocation(nativeContext->programVOES, "position"));
    assert(nativeContext->positionHandleVOES != -1);
    nativeContext->samplerHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "sampler"));
    assert(nativeContext->samplerHandleVOES != -1);
    nativeContext->vertTransformHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "vertTransform"));
    assert(nativeContext->vertTransformHandleVOES != -1);
    nativeContext->heightHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "height"));
    assert(nativeContext->heightHandleVOES != -1);
    nativeContext->lodHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "lod"));
    assert(nativeContext->lodHandleVOES != -1);
    nativeContext->minLodHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "minLod"));
    assert(nativeContext->minLodHandleVOES != -1);
    nativeContext->contrastingColorHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "contrastingColor"));
    assert(nativeContext->contrastingColorHandleVOES != -1);
    nativeContext->contrastingColorMixHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "contrastingColorMix"));
    assert(nativeContext->contrastingColorMixHandleVOES != -1);
    nativeContext->texTransformHandleVOES =
            CHECK_GL(glGetUniformLocation(nativeContext->programVOES, "texTransform"));
    assert(nativeContext->texTransformHandleVOES != -1);

    nativeContext->programH = CreateGlProgram(VERTEX_SHADER_SRC_NO_TRANSFORM,
                                              FRAGMENT_SHADER_SRC_H);
    assert(nativeContext->programH);
    nativeContext->positionHandleH =
            CHECK_GL(glGetAttribLocation(nativeContext->programH, "position"));
    assert(nativeContext->positionHandleH != -1);
    nativeContext->samplerHandleH =
            CHECK_GL(glGetUniformLocation(nativeContext->programH, "sampler"));
    assert(nativeContext->samplerHandleH != -1);
    nativeContext->widthHandleH =
            CHECK_GL(glGetUniformLocation(nativeContext->programH, "width"));
    assert(nativeContext->widthHandleH != -1);
    nativeContext->lodHandleH =
            CHECK_GL(glGetUniformLocation(nativeContext->programH, "lod"));
    assert(nativeContext->lodHandleH != -1);
    nativeContext->minLodHandleH =
            CHECK_GL(glGetUniformLocation(nativeContext->programH, "minLod"));
    assert(nativeContext->minLodHandleH != -1);
    nativeContext->contrastingColorHandleH =
            CHECK_GL(glGetUniformLocation(nativeContext->programH, "contrastingColor"));
    assert(nativeContext->contrastingColorHandleH != -1);
    nativeContext->contrastingColorMixHandleH =
            CHECK_GL(glGetUniformLocation(nativeContext->programH, "contrastingColorMix"));
    assert(nativeContext->contrastingColorMixHandleH != -1);

    nativeContext->programV2D = CreateGlProgram(VERTEX_SHADER_SRC_NO_TRANSFORM,
                                                FRAGMENT_SHADER_SRC_V_2D);
    assert(nativeContext->programV2D);
    nativeContext->positionHandleV2D =
            CHECK_GL(glGetAttribLocation(nativeContext->programV2D, "position"));
    assert(nativeContext->positionHandleV2D != -1);
    nativeContext->samplerHandleV2D =
            CHECK_GL(glGetUniformLocation(nativeContext->programV2D, "sampler"));
    assert(nativeContext->samplerHandleV2D != -1);
    nativeContext->heightHandleV2D =
            CHECK_GL(glGetUniformLocation(nativeContext->programV2D, "height"));
    assert(nativeContext->heightHandleV2D != -1);
    nativeContext->lodHandleV2D =
            CHECK_GL(glGetUniformLocation(nativeContext->programV2D, "lod"));
    assert(nativeContext->lodHandleV2D != -1);
    nativeContext->minLodHandleV2D =
            CHECK_GL(glGetUniformLocation(nativeContext->programV2D, "minLod"));
    assert(nativeContext->minLodHandleV2D != -1);
    nativeContext->contrastingColorHandleV2D =
            CHECK_GL(glGetUniformLocation(nativeContext->programV2D, "contrastingColor"));
    assert(nativeContext->contrastingColorHandleV2D != -1);
    nativeContext->contrastingColorMixHandleV2D =
            CHECK_GL(glGetUniformLocation(nativeContext->programV2D, "contrastingColorMix"));
    assert(nativeContext->contrastingColorMixHandleV2D != -1);

    CHECK_GL(glGenTextures(1, &(nativeContext->inputTextureId)));

    return reinterpret_cast<jlong>(nativeContext);
}

JNIEXPORT jboolean JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_setWindowSurface(
        JNIEnv *env, jobject clazz, jlong context, jobject jsurface) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);

    // Destroy previously connected surface
    DestroySurface(nativeContext);

    // Null surface may have just been passed in to destroy previous surface.
    if (!jsurface) {
        return JNI_FALSE;
    }

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, jsurface);
    if (nativeWindow == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to set window surface: Unable to "
                                                        "acquire native window.");
        return JNI_FALSE;
    }

    EGLSurface surface =
            eglCreateWindowSurface(nativeContext->display, nativeContext->config,
                                   nativeWindow, /*attrib_list=*/nullptr);
    assert(surface != EGL_NO_SURFACE);

    nativeContext->windowSurface = std::make_pair(nativeWindow, surface);
    eglMakeCurrent(nativeContext->display, surface, surface, nativeContext->context);

    auto width = ANativeWindow_getWidth(nativeWindow);
    auto height = ANativeWindow_getHeight(nativeWindow);
    CHECK_GL(glViewport(0, 0, width, height));

    InitFrameBuffer(&(nativeContext->pass1TextureId),
                    &(nativeContext->fbo1Id),
                    width / 2,
                    height / 2);
    InitFrameBuffer(&(nativeContext->pass2TextureId),
                    &(nativeContext->fbo2Id),
                    width / 2,
                    height / 2);
    InitFrameBuffer(&(nativeContext->pass3TextureId),
                    &(nativeContext->fbo3Id),
                    width / 4,
                    height / 4);
    InitFrameBuffer(&(nativeContext->pass4TextureId),
                    &(nativeContext->fbo4Id),
                    width / 4,
                    height / 4);
    InitFrameBuffer(&(nativeContext->pass5TextureId),
                    &(nativeContext->fbo5Id),
                    width / 2,
                    height / 2);
    InitFrameBuffer(&(nativeContext->pass6TextureId),
                    &(nativeContext->fbo6Id),
                    width / 2,
                    height / 2);
    InitFrameBuffer(&(nativeContext->pass7TextureId),
                    &(nativeContext->fbo7Id),
                    width,
                    height);

    glEnable(GL_SCISSOR_TEST);
    CHECK_GL(glScissor(0, 0, width, height));

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_getTexName(
        JNIEnv *env, jobject clazz, jlong context) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    return nativeContext->inputTextureId;
}

JNIEXPORT jboolean JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_renderTexture(
        JNIEnv *env, jobject clazz, jlong context, jlong timestampNs,
        jfloatArray jvertTransformArray, jfloatArray jtexTransformArray,
        jfloatArray jrectsCoordinates, jint jallRectsCount, jint jotherRectsCount) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    auto nativeWindow = nativeContext->windowSurface.first;

    GLfloat *vertTransformArray = env->GetFloatArrayElements(jvertTransformArray, nullptr);
    GLfloat *texTransformArray = env->GetFloatArrayElements(jtexTransformArray, nullptr);

    auto width = ANativeWindow_getWidth(nativeWindow);
    auto height = ANativeWindow_getHeight(nativeWindow);
    CHECK_GL(glScissor(0, 0, width, height));

    CHECK_GL(glEnable(GL_STENCIL_TEST));
    CHECK_GL(glClear(GL_STENCIL_BUFFER_BIT));
    CHECK_GL(glDisable(GL_STENCIL_TEST));

    if (nativeContext->blurEnabled || nativeContext->IsAnimatingLod()) {
        if (nativeContext->IsAnimatingLod()) nativeContext->AnimateLod();
        nativeContext->DrawBlur(vertTransformArray, texTransformArray, (GLfloat) width,
                                (GLfloat) height);
    } else {
        nativeContext->DrawNoBlur(vertTransformArray, texTransformArray, (GLfloat) width,
                                  (GLfloat) height, 0, 0, .0f);
    }

    GLfloat *rectsCoordinates =
            jallRectsCount == 0
            ? nullptr
            : env->GetFloatArrayElements(jrectsCoordinates, nullptr);
    if (rectsCoordinates != nullptr) {
        if (nativeContext->IsAnimatingContrastingColor()) {
            nativeContext->AnimateContrastingColor();
        }

        const GLuint rectsCount = !nativeContext->blurEnabled ||
                                  nativeContext->IsAnimatingLod()
                                  ? jallRectsCount : jotherRectsCount;
        nativeContext->DrawBlurredRects(vertTransformArray, texTransformArray,
                                        rectsCoordinates, rectsCount,
                                        (GLfloat) width, (GLfloat) height);
        env->ReleaseFloatArrayElements(jrectsCoordinates, rectsCoordinates,
                                       JNI_ABORT);
    }

    env->ReleaseFloatArrayElements(jvertTransformArray, vertTransformArray, JNI_ABORT);
    env->ReleaseFloatArrayElements(jtexTransformArray, texTransformArray, JNI_ABORT);

    // Check that all GL operations completed successfully. If not, log an error and return.
    GLenum glError = glGetError();
    if (glError != GL_NO_ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                            "Failed to draw frame due to OpenGL error: %s",
                            GLErrorString(glError).c_str());
        return JNI_FALSE;
    }

// Only attempt to set presentation time if EGL_EGLEXT_PROTOTYPES is defined.
// Otherwise, we'll ignore the timestamp.
#ifdef EGL_EGLEXT_PROTOTYPES
    eglPresentationTimeANDROID(nativeContext->display,
                               nativeContext->windowSurface.second, timestampNs);
#endif  // EGL_EGLEXT_PROTOTYPES
    EGLBoolean swapped = eglSwapBuffers(nativeContext->display,
                                        nativeContext->windowSurface.second);
    if (!swapped) {
        EGLenum eglError = eglGetError();
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                            "Failed to swap buffers with EGL error: %s",
                            EGLErrorString(eglError).c_str());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_setBlurEnabled(
        JNIEnv *env, jobject clazz, jlong context, jboolean enabled, jboolean animated) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    if (nativeContext->blurEnabled == enabled) return;

    nativeContext->blurEnabled = enabled;
    if (enabled && nativeContext->currentBlurAnimationFrame == -1) {
        if (animated) {
            nativeContext->currentBlurAnimationFrame = 0;
        } else {
            nativeContext->currentBlurAnimationFrame = NativeContext::BLUR_ANIMATION_FRAMES;
            nativeContext->lod = NativeContext::MAX_LOD;
            nativeContext->contrastingColorMix = NativeContext::MIN_CONTRASTING_COLOR_MIX;
        }
    } else if (!enabled &&
               nativeContext->currentBlurAnimationFrame == NativeContext::BLUR_ANIMATION_FRAMES) {
        if (animated) {
            nativeContext->currentBlurAnimationFrame = NativeContext::BLUR_ANIMATION_FRAMES - 1;
        } else {
            nativeContext->currentBlurAnimationFrame = -1;
            nativeContext->lod = NativeContext::MIN_LOD;
            nativeContext->contrastingColorMix = NativeContext::MAX_CONTRASTING_COLOR_MIX;
        }
    }
}

JNIEXPORT void JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_setContrastingColor(
        JNIEnv *env, jobject clazz, jlong context,
        jfloat red, jfloat green, jfloat blue) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    nativeContext->targetContrastingRed = red;
    nativeContext->targetContrastingGreen = green;
    nativeContext->targetContrastingBlue = blue;
    if (nativeContext->contrastingRed == -1.f
        && nativeContext->contrastingGreen == -1.f
        && nativeContext->contrastingBlue == -1.f) {
        nativeContext->contrastingRed = red;
        nativeContext->contrastingGreen = green;
        nativeContext->contrastingBlue = blue;
    } else {
        nativeContext->currentContrastingColorAnimationFrame = 0;
    }
}

JNIEXPORT void JNICALL
Java_com_lookaround_core_android_camera_OpenGLRenderer_closeContext(
        JNIEnv *env, jobject clazz, jlong context) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);

    if (nativeContext->programVOES) {
        CHECK_GL(glDeleteProgram(nativeContext->programVOES));
        nativeContext->programVOES = 0;
    }

    if (nativeContext->programH) {
        CHECK_GL(glDeleteProgram(nativeContext->programH));
        nativeContext->programH = 0;
    }

    if (nativeContext->programV2D) {
        CHECK_GL(glDeleteProgram(nativeContext->programV2D));
        nativeContext->programV2D = 0;
    }

    DestroySurface(nativeContext);
    eglDestroySurface(nativeContext->display, nativeContext->bufferSurface);
    eglMakeCurrent(nativeContext->display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(nativeContext->display, nativeContext->context);
    eglTerminate(nativeContext->display);

    delete nativeContext;
}
}// extern "C"

#undef CHECK_GL
