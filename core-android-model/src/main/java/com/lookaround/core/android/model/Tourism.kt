package com.lookaround.core.android.model

import android.os.Parcelable
import com.lookaround.core.model.IPlaceType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Tourism(
    override val description: String,
    override val count: Int,
) : IPlaceType, Parcelable {
    INFORMATION("Information for tourists and visitors, including information offices", 839906),
    HOTEL("Hotel - an establishment that provides paid lodging", 367874),
    ATTRACTION(
        "An object of interest for a tourist, or a purpose-built tourist attraction",
        207955
    ),
    VIEWPOINT(
        "A place worth visiting, often high, with a good view of surrounding countryside or notable buildings.",
        189729
    ),
    ARTWORK("A public piece of art.", 183356),
    GUEST_HOUSE(
        "Accommodation smaller than a hotel and typically owner-operated, such as bed&breakfasts etc.",
        152494
    ),
    PICNIC_SITE(
        "A locality that is suitable for outdoors eating, with facilities to aid a picnic such as tables and benches",
        140384
    ),
    CAMP_SITE(
        "An area where people can camp overnight using tents, camper vans or caravans.",
        122374
    ),
    MUSEUM(
        "A museum: an institution with exhibitions on scientific, historical, cultural topics",
        89228
    ),
    CHALET("A holiday cottage with self-contained cooking and bathroom facilities.", 63016),
    APARTMENT(
        "A furnished apartment or flat with cooking and bathroom facilities that can be rented for holiday vacations.",
        58082
    ),
    HOSTEL("Cheap accommodation with shared bedrooms.", 51321),
    MOTEL("Short term accommodation, particularly for people travelling by car.", 43281),
    CAMP_PITCH("A tent or caravan pitch location within a campsite or caravan site", 40384),
    CARAVAN_SITE("A place where you can stay in a caravan overnight or for longer periods.", 29199),
    ALPINE_HUT(
        "A remote building located in the mountains intended to provide board and lodging.",
        13593
    ),
    WILDERNESS_HUT(
        "A remote building, with generally a fireplace, intended to provide temporary shelter and sleeping accommodation, typically un-serviced and with no staff.",
        11106
    ),
    GALLERY("An area or building that displays a variety of visual art exhibitions.", 10540),
    THEME_PARK(
        "An amusement park where entertainment is provided by rides, games, concessions.",
        8530
    ),
    ZOO("A zoological garden, where animals are confined for viewing by the public.", 7294),
    AQUARIUM("A facility with living aquatic animals for public viewing.", 1107);
}
