package com.lookaround.core.android.model

import android.os.Parcelable
import com.lookaround.core.model.IPlaceType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Healthcare(
    override val description: String,
    override val count: Int,
) : IPlaceType, Parcelable {
    PHARMACY(
        "A pharmacy: a shop where a pharmacist sells prescription medications. Used in addition to amenity=pharmacy",
        169240,
    ),
    HOSPITAL(
        "A hospital providing in-patient medical treatment, used in addition to amenity=hospital",
        79483,
    ),
    DOCTOR(
        "A place to get medical attention or a check up from a physician. Used in addition to amenity=doctors",
        77175,
    ),
    CLINIC(
        "A medical facility, with more staff than a doctor's office, that does not admit inpatients. Used in addition to amenity=clinic",
        69984,
    ),
    DENTIST(
        "A dentist practice / surgery. Used in addition to amenity=dentist",
        57653,
    ),
    CENTRE(
        "Unspecific healthcare facility. Usage without further specification is discouraged.",
        47263,
    ),
    PHYSIOTHERAPIST(
        "A facility where a physiotherapist practices physical therapy (kinesiology, exercise, mobilization, etc).",
        12816,
    ),
    YES(
        "Unspecific healthcare object. Usage without further specification is discouraged.",
        11525,
    ),
    ALTERNATIVE(
        "A facility where alternative or complementary medicine is practiced: e.g. acupuncture, chiropractic, naturopathy, etc.",
        10323,
    ),
    LABORATORY(
        "A medical laboratory, a place which performs analysis and diagnostics on body fluids",
        9697,
    ),
    PSYCHOTHERAPIST(
        "An office of a psychotherapist or clinical psychologist.",
        4429,
    ),
    OPTOMETRIST(
        "An optometrist's office",
        2990,
    ),
    REHABILITATION(
        "Medical rehabilitation facility, usually inpatient or residential",
        2630,
    ),
    PODIATRIST(
        "A podiatrist's office",
        2354,
    ),
    NURSE(
        "A facility where a trained health professional (nurse) provides healthcare, who is not a physician",
        1610,
    ),
    SPEECH_THERAPIST(
        "A speech therapist, a health specialist who deals with speech, voice, swallowing or hearing impairment.",
        1472,
    ),
    COUNSELLING(
        "A facility where health counselling is provided, e.g. dietitian, nutrition, addiction.",
        1459,
    ),
    HOSPICE(
        "A hospice which provides palliative care to terminally ill people and support to their relatives",
        1346,
    ),
    SAMPLE_COLLECTION(
        "Site or dedicated healthcare facility where samples of blood/urine/etc are obtained or collected for purpose of analyzing them for healthcare diagnostics",
        1314,
    ),
    BLOOD_DONATION(
        "Facility where you can donate blood, plasma and/or platelets, and possibly have stem cell samples taken",
        1226,
    ),
    OCCUPATIONAL_THERAPIST(
        "A facility where an occupational therapist practices.",
        949,
    ),
    MIDWIFE(
        "A facility where healthcare is provided by a midwife, a health professional who cares for mothers and newborns around childbirth",
        851,
    )
}
