import com.github.filosganga.geogson.gson.GeometryAdapterFactory
import com.google.gson.GsonBuilder
import com.lookaround.core.di.HttpModule
import com.lookaround.repo.photon.PhotonEndpoints
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PhotonRepoTests {
    private val converterFactory = GsonConverterFactory.create(GsonBuilder()
        .registerTypeAdapterFactory(GeometryAdapterFactory())
        .create())
    private val httpClient = HttpModule.testHttpClient(HttpModule.httpLoggingInterceptor())
    private val endpoints = Retrofit.Builder()
        .baseUrl(PhotonEndpoints.BASE_URL)
        .addConverterFactory(converterFactory)
        .client(httpClient)
        .build()
        .create(PhotonEndpoints::class.java)

    @Test
    fun t() {
        runBlocking { println(endpoints.search("Berlin").toString()) }
    }
}