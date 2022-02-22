package com.lookaround.core.android.model

import android.os.Parcelable
import com.lookaround.core.model.IPlaceType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Leisure(
    override val description: String,
    override val count: Int,
) : IPlaceType, Parcelable {
    PITCH(
        "An area designed for practising a particular sport, normally designated with appropriate markings.",
        1958503
    ),
    SWIMMING_POOL("A swimming pool (water area only)", 1484473),
    PARK("A park, usually urban (municipal).", 1072962),
    GARDEN(
        "A place where flowers and other plants are grown in a decorative and structured manner or for scientific purposes.",
        828693
    ),
    PLAYGROUND("A playground: an area designed for children to play.", 694044),
    SPORTS_CENTRE(
        "A sports centre is a distinct facility where sports take place within an enclosed area.",
        211624
    ),
    PICNIC_TABLE("A table with benches for food and rest", 199484),
    NATURE_RESERVE(
        "A protected area of importance for wildlife, flora, fauna or features of geological or other special interest.",
        115814
    ),
    TRACK(
        "A track for running, cycling and other non-motorised racing such as horses, greyhounds.",
        100637
    ),
    FITNESS_CENTRE(
        "Fitness centre, health club or gym with exercise machines, fitness classes or both, for exercise.",
        56178
    ),
    STADIUM("A major sports facility with substantial tiered seating.", 48292),
    SLIPWAY("A slipway: a ramp for launching a boat into water", 44489),
    FITNESS_STATION(
        "An outdoor facility where people can practise typical fitness exercises",
        41867
    ),
    GOLF_COURSE("A golf course", 39470),
    COMMON(
        "Identify land over which the public has general rights of use for certain leisure activities.",
        37392
    ),
    OUTDOOR_SEATING(
        "A seating area, usually for the consumption of food and drink from neighbouring cafes and restaurants, often belonging to one or more of them, but not necessarily adjacent.",
        26953
    ),
    MARINA("A facility for mooring leisure yachts and motor boats.", 26300),
    FIREPIT("A fire ring or fire pit, often at a campsite or picnic site", 23701),
    BLEACHERS("Raised, tiered rows of benches found at spectator events", 20096),
    RECREATION_GROUND("See also: tag:landuse=recreation_ground", 17515),
    DOG_PARK(
        "A designated area, with or without a fenced boundary, where dog-owners are permitted to exercise their pets unrestrained.",
        17115
    ),
    HORSE_RIDING(
        "A facility where people practise horse riding, usually in their spare time, e.g. a riding centre. For a riding arena use tag:leisure=pitch + tag:sport=equestrian.",
        14655
    ),
    WATER_PARK(
        "An amusement park with features like water slides, recreational pools (e.g. wave pools) or lazy rivers.",
        11929
    ),
    ICE_RINK("A place where you can skate and play bandy or ice hockey.", 7466),
}
