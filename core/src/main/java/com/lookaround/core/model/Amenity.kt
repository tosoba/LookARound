package com.lookaround.core.model

import java.util.*

enum class Amenity(override val description: String, override val count: Int) : IPlaceType {
    PARKING("A place for parking cars", 3784858),
    BENCH("A place for people to sit; allows room for several people.", 1448454),
    PLACE_OF_WORSHIP("A place where religious services are conducted", 1242549),
    RESTAURANT(
        "A restaurant sells full sit-down meals with servers, and may sell alcohol.",
        1149907
    ),
    SCHOOL("A primary or secondary school (pupils typically aged 6 to 18)", 1103530),
    PARKING_SPACE("A single parking space on a parking lot", 648006),
    FUEL("A retail facility for refueling motor vehicles", 458046),
    WASTE_BASKET(
        "A single small container for depositing garbage that is easily accessible for pedestrians.",
        451387
    ),
    CAFE(
        "A generally informal place with sit-down facilities selling beverages and light meals and/or snacks.",
        439581
    ),
    FAST_FOOD(
        "A place concentrating on very fast counter-only service and take-away food.",
        422112
    ),
    BICYCLE_PARKING("A parking space designed for bicycles.", 362462),
    BANK(
        "A financial establishment where customers can, among other services, deposit money and take loans.",
        361398
    ),
    SHELTER("A small structure for protection against bad weather conditions", 337638),
    TOILETS("A publicly accessible toilet", 320693),
    PHARMACY("A shop where a pharmacist sells medications", 312765),
    POST_BOX("A box to deposit outgoing postal items.", 309317),
    RECYCLING("A container or centre that accepts waste for recycling.", 297989),
    KINDERGARTEN(
        "A place for looking after preschool children and (typically) giving early education.",
        257355
    ),
    DRINKING_WATER("A drinking water source which provides potable water for consumption.", 229455),
    HOSPITAL("A hospital providing in-patient medical treatment", 198502),
    BAR(
        "An establishment that sells alcoholic drinks to be consumed on the premises, characterised by a noisy and vibrant atmosphere. They usually do not sell food to be eaten as a meal. The music is usually loud and you often have to stand.",
        190259
    ),
    ATM(
        "A device that provides the clients of a financial institution with access to financial transactions.",
        181837
    ),
    GRAVE_YARD(
        "A place where people (or sometimes animals) are buried which is close to a place of worship.",
        174895
    ),
    POST_OFFICE("A place where letters and parcels may be sent or collected.", 173570),
    VENDING_MACHINE(
        "A vending machine sells food, drinks, tickets or other goods automatically.",
        170989
    ),
    PUB(
        "An establishment that sells alcoholic drinks that can be consumed on the premises, characterised by a traditional appearance and a relaxed atmosphere. Also sells food which also can be eaten on the premises. Customers can usually sit down at a table.",
        169463
    ),
    HUNTING_STAND(
        "An open or enclosed platform used by hunters to place themselves at an elevated height above the terrain.",
        160297
    ),
    CLINIC(
        "A clinic is a medical centre, with more staff than a doctor's office, that does not admit inpatients.",
        139327
    ),
    POLICE(
        "A police station is a facility where police officers patrol from and that is a first point of contact for civilians.",
        131378
    ),
    DOCTORS(
        "A doctor's office, a place to get medical attention or a check up from a physician.",
        130871
    ),
    FOUNTAIN(
        "A fountain with cultural, decorational or historical significance or which serves a recreational purpose.",
        129917
    ),
    TOWNHALL("A townhall serves as a community administrative center or meeting place.", 129057),
    COMMUNITY_CENTRE(
        "A place mostly used for local events, festivities and group activities.",
        124617
    ),
    WASTE_DISPOSAL(
        "A medium or large disposal bin, typically for bagged up household or industrial waste.",
        121320
    ),
    FIRE_STATION("A station from which a fire brigade operates.", 115788),
    SOCIAL_FACILITY("A facility that provides social services", 108331),
    PARKING_ENTRANCE("A entrance to an underground or multi storey parking facility", 98764),
    TELEPHONE("A public telephone", 97841),
    DENTIST("A dentist practice / surgery.", 94052),
    LIBRARY("A place to read and/or lend books", 85967),
    CAR_WASH("A car wash station", 81782),
    MARKETPLACE("A marketplace where goods and services are traded daily or weekly.", 66101),
    BUS_STATION(
        "A bus station would usually be a terminus where many routes stop / start, and where you can change between routes, etc.",
        61805
    ),
    COLLEGE(
        "A place for further education, a post-secondary education institution which is not a University",
        61437
    ),
    UNIVERSITY(
        "An educational institution designed for instruction, examination, or both, of students in many branches of advanced learning.",
        51151
    ),
    CHARGING_STATION("A station that supplies energy to electrical vehicles.", 48320),
    BICYCLE_RENTAL(
        "A place, usually unattended, where you can pick up and drop off rented bikes.",
        43904
    ),
    VETERINARY(
        "A place that deals with the prevention, diagnosis and treatment of disease in animals.",
        41011
    ),
    THEATRE("A place where live theatrical performances are held.", 38448),
    TAXI(
        "A place where taxis wait for passengers. Often near where many people congregate.",
        37914
    ),
    ICE_CREAM("A place that sells ice cream and/or frozen yoghurt over the counter.", 33368),
    CINEMA("A movie theater, a place showing movies.", 28665),
    WATER_POINT(
        "A place where you can get large amounts of drinking water for refilling fresh water holding tanks.",
        26366
    ),
    BBQ("A permanently built place for having a BBQ", 24606),
    FERRY_TERMINAL("A place where people/cars/etc can board and leave a ferry.", 24572),
    COURTHOUSE(
        "A building which is home to a court of law, which administers justice according to the rule of law.",
        24406
    ),
    CHILDCARE(
        "A place where children are looked after which is not an amenity=kindergarten",
        24088
    ),
    CAR_RENTAL("A place from which cars can be rented.", 22676),
    DRIVING_SCHOOL("A school to learn to drive a motor vehicle", 21785),
    NIGHTCLUB("A place to dance and drink at night.", 21257),
    ARTS_CENTRE(
        "An arts centre. A venue where a variety of arts are performed or conducted",
        21164
    ),
    SHOWER("Public showers, where people can bathe under water sprays", 20992),
    MOTORCYCLE_PARKING("A place which is designated for parking motorcycles", 19914),
    GRIT_BIN(
        "A container that holds sand, salt and gravel to spread on roads for winter safety",
        18801
    ),
    CLOCK("A publicly visible clock.", 18552),
    NURSING_HOME("A home for disabled/elderly persons who need permanent care.", 14469),
    BUREAU_DE_CHANGE("An office that exchanges foreign currency and travellers cheques.", 13474),
    PRISON(
        "A prison or jail where people are incarcerated before trial or after conviction",
        11449
    ),
    INTERNET_CAFE("A place whose principal role is providing internet services.", 11409),
    WATERING_PLACE("A place where water is contained and animals such as horses can drink.", 11054),
    BIERGARTEN(
        "An open-air area where beer is served and you are allowed to bring your own food.",
        10495
    ),
    MOBILE_MONEY_AGENT("A place where mobile money can be payed in and out.", 10284),
    FOOD_COURT(
        "A place with sit-down facilities shared by multiple self-service food vendors.",
        9764
    ),
    STUDIO(
        "A studio used for creating radio or television programmes and broadcasting them. It can also be used to mark a music recording studio.",
        9432
    ),
    PUBLIC_BOOKCASE("A street furniture containing books. Take one, leave one.", 8737),
    EVENTS_VENUE(
        "Used to identify places which provide facilities for events such as banquets, weddings, etc.",
        8568
    ),
    PUBLIC_BATH("A location where the public may bathe in common", 8462),
    CAR_SHARING(
        "Carsharing station, where you get your booked car, often separate areas on parking places.",
        8295
    ),
    BICYCLE_REPAIR_STATION("A public tool for self-repair of bicycles.", 7234),
    SWIMMING_POOL("DISCOURAGED, use tag:leisure=swimming_pool instead", 7166),
    WASTE_DUMP_SITE("A place where trash was illegally dumped", 6991),
    CASINO("A gambling venue with at least one table game.", 6247),
    LOADING_DOCK("A loading dock.", 6096),
    MONASTERY(
        "An active monastery or convent, occupied by a community of monks or nuns living under religious vows",
        6025
    ),
    LETTER_BOX(
        "Private mailboxes where mailmen or other people deposit letters to specific addresses.",
        5897
    ),
    SOCIAL_CENTRE(
        "A centre of fraternities, sororities, professional societies, union halls and other nonprofit organization.",
        5535
    ),
    VEHICLE_INSPECTION("Location where legally-required vehicle inspection may be performed", 5532),
    COMPRESSED_AIR("A device to inflate tires/tyres (e.g. motorcar, bicycle).", 5086),
    MUSIC_SCHOOL(
        "Music school, an educational institution specialized in the study, training, and research of music",
        4931
    ),
    SANITARY_DUMP_STATION("Place for depositing human waste from a toilet holding tank.", 4633),
    DOJO("A formal training place for any of the Japanese do arts.", 4310),
    EMBASSY("Formerly used tag to map a representation of a country in another country.", 4231),
    PAYMENT_TERMINAL("Self-service payment kiosk/terminal", 4181),
    ANIMAL_SHELTER("A shelter for animal recovery", 4083),
    ANIMAL_BREEDING("A facility where animals are bred, usually to sell them", 4060),
    LAVOIR("A public place where people come to wash their clothes manually", 4001),
    MONEY_TRANSFER("A place that offers money transfers, especially cash to cash", 3914),
    WASTE_TRANSFER_STATION(
        "A location that accepts, consolidates and transfers waste in bulk, usually from government or commercial collections.",
        3588
    ),
    BOAT_STORAGE("A place to store boats out of the water.", 3389),
    BOAT_RENTAL("A place where you can rent a boat.", 3292),
    LANGUAGE_SCHOOL("Educational institution where one studies a foreign language", 3218),
    GAME_FEEDING("A game feeding place", 3094),
    CONFERENCE_CENTRE("A conference centre is a large building used to hold a convention", 3084),
    RANGER_STATION(
        "An official park visitor facility with police, visitor information, permit services, etc.",
        3061
    ),
    LOVE_HOTEL(
        "A love hotel is a type of short-stay hotel operated primarily for the purpose of allowing guests privacy for sexual activities.",
        2992
    ),
    FEEDING_PLACE(
        "A place where animals are fed like a manger or an automated feeding station.",
        2982
    ),
    WEIGHBRIDGE("A large weight scale to weigh vehicles and goods", 2920),
    GAMBLING(
        "A place for gambling, not being a bookmaker, lottery shop, casino, or adult gaming centre.",
        2844
    ),
    BROTHEL("An establishment specifically dedicated to prostitution.", 2782),
    PREP_SCHOOL("Tutor or test prep centre", 2482),
    ANIMAL_BOARDING(
        "A facility which cares for pets while the owners are away (e.g. on holiday)",
        2476
    ),
    TRAINING("Public places where you can get training", 2328),
    TICKET_VALIDATOR("A device which validates public transport tickets", 2272),
    BLOOD_BANK(
        "A center where blood gathered as a result of blood donation is stored and preserved for later use in blood transfusion",
        2160
    ),
    SMOKING_AREA("To identify a designated smoking area", 2059),
    CREMATORIUM("A tag for crematorium, where bodies are burned.", 1883),
    TROLLEY_BAY("A trolley bay / cart corral is the place where trolleys are \"parked\"", 1850),
    KNEIPP_WATER_CURE("A foot bath is a shallow pool, often with handrail.", 1796),
    OFFICE("DO NOT USE! See office", 1790),
    RESEARCH_INSTITUTE(
        "A research institute is an establishment endowed for doing research.",
        1624
    ),
    HEALTH_POST("A village or neighborhood health post, without a physician", 1621),
    PHOTO_BOOTH("A stand to create instant photos.", 1565),
    POST_DEPOT("A distribution centre or sorting office for letters and parcels.", 1560),
    COWORKING_SPACE(
        "A place where people can go to work (might require a fee); not limited to a single employer",
        1441
    ),
    STABLES("Riding stables, equestrian center", 1255),
    MORTUARY("A morgue or funeral home, used for the storage of human corpses", 1254),
    RECEPTION_DESK("First contact point of and information source for a POI for an outsider", 1253),
    VACUUM_CLEANER("A device used to clean motor vehicles by vacuum.", 1250),
    DIVE_CENTRE("The base location where sports divers usually start scuba diving", 1220),
    CAR_POOLING(
        "Car pooling station or spot, where you meet to get in (or where you get dropped off) someone's car or to pick up (or drop off) someone.",
        1172
    ),
    PAYMENT_CENTRE(
        "A non-bank place, where people can pay bills of public and private services and taxes.",
        1061
    ),
    DRESSING_ROOM("A place where people can change their clothes.", 1040),
    STRIPCLUB("A place that offer striptease and lap dances.", 988),
    FUNERAL_HALL("A place for holding a funeral ceremony, other than a place of worship", 949),
    KARAOKE_BOX(
        "A venue specifically for people to enjoy singing along with karaoke, usually with private booths.",
        912
    ),
    TABLE("A public table", 863),
    HOOKAH_LOUNGE("A place where guests can smoke hookah (aka waterpipe, nargile, shisha).", 797),
    LOUNGER("An object for people to lie down.", 792),
    POLLING_STATION("A place where one can cast a ballot.", 769);

    override val label: String
        get() =
            name.replace("_", " ").toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault())

    override val typeKey: String
        get() = "amenity"

    override val typeValue: String
        get() = name.toLowerCase(Locale.getDefault())
}
