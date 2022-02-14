package com.lookaround.core.android.model

import android.os.Parcelable
import com.lookaround.core.model.IPlaceType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Shop(
    override val description: String,
    override val count: Int,
) : IPlaceType, Parcelable {
    CONVENIENCE(
        "A convenience shop is a small local shop carrying a variety of everyday products, such as packaged food and hygiene products",
        623123,
    ),
    SUPERMARKET(
        "A large shop selling groceries, fresh produce, and other goods.",
        420277,
    ),
    CLOTHES(
        "A shop which primarily sells clothing",
        319724,
    ),
    HAIRDRESSER(
        "A hairdressers or barbers shop, where hair is cut",
        261570,
    ),
    CAR_REPAIR(
        "A business where cars are repaired.",
        207915,
    ),
    BAKERY(
        "A shop selling bread",
        201593,
    ),
    YES(
        "A shop of unspecified type or indicator that feature such as fuel station has shop.",
        177130,
    ),
    CAR(
        "A place that primarily sells cars (automobiles)",
        127248,
    ),
    BEAUTY(
        "A non-hairdresser beauty shop, spa, nail salon, etc.",
        102227,
    ),
    KIOSK(
        "A small shop on the pavement that sells magazines, tobacco, newspapers, sweets and stamps.",
        85273,
    ),
    BUTCHER(
        "A shop selling meat or meat products.",
        80434,
    ),
    HARDWARE(
        "A shop which sells timber, tools and other building products",
        79731,
    ),
    FURNITURE(
        "A shop selling furniture.",
        75413,
    ),
    MOBILE_PHONE(
        "A shop that primarily sells mobile phones and accessories.",
        74755,
    ),
    CAR_PARTS(
        "A place selling auto parts, auto accessories, motor oil, car chemicals, etc.",
        68734,
    ),
    ALCOHOL(
        "A shop selling alcoholic drinks",
        66745,
    ),
    FLORIST(
        "A shop selling bouquets of flowers",
        66668,
    ),
    ELECTRONICS(
        "A shop selling consumer electronics such as TVs, radios and fridges.",
        66626,
    ),
    SHOES(
        "A shop selling shoes",
        63930,
    ),
    MALL(
        "A group of stores, typically associated with a single building structure.",
        62652,
    ),
    VARIETY_STORE(
        "A variety store or price-point retailer is a retail shop that sells inexpensive items",
        56666,
    ),
    DOITYOURSELF(
        "A Do-it-Yourself-store, a large hardware and home improvement shop",
        56242,
    ),
    OPTICIAN(
        "A shop that sells, fits, and repairs prescription eyeglasses and contact lenses.",
        55834,
    ),
    JEWELRY(
        "A shop that sells rings, necklaces, earrings, watches, etc.",
        53962,
    ),
    GIFT(
        "Shop selling gifts, greeting cards, or tourist gifts (souvenirs).",
        51413,
    ),
    GREENGROCER(
        "A shop which sells fruits and vegetables",
        48672,
    ),
    DEPARTMENT_STORE(
        "A large store with multiple clothing and other general merchandise departments.",
        46940,
    ),
    BOOKS(
        "A store specializing in the sale of books, although it may also sell other printed publications, such as newspapers and magazines",
        46924,
    ),
    BICYCLE(
        "A store where you can buy and/or repair your bike and buy accessories",
        46316,
    ),
    CHEMIST(
        "A shop selling articles of personal hygiene, cosmetics, and household cleaning products.",
        40719,
    ),
    TRAVEL_AGENCY(
        "A shop selling travel related products and services",
        40422,
    ),
    SPORTS(
        "A shop selling sports equipment and clothing.",
        36841,
    ),
    LAUNDRY(
        "A shop to wash clothes and bedding, generally self-service and unattended.",
        36705,
    ),
    CONFECTIONERY(
        "A shop selling sweets and candies.",
        33481,
    ),
    STATIONERY(
        "A shop selling office supplies",
        33385,
    ),
    PET(
        "A shop selling pets and/or pet supplies",
        33293,
    ),
    VACANT(
        "Shop that is currently not being used.",
        30195,
    ),
    COMPUTER(
        "A shop selling computers, peripherals, software, ...",
        30031,
    ),
    TYRES(
        "A shop selling tyres.",
        27756,
    ),
    NEWSAGENT(
        "A shop selling newspapers and magazines.",
        26453,
    ),
    BEVERAGES(
        "Shop focused on selling beverages and drinks, possibly including alcoholic and non-alcoholic ones.",
        26398,
    ),
    DRY_CLEANING(
        "A shop or kiosk offering a clothes dry cleaning service.",
        26109,
    ),
    COSMETICS(
        "Shop exclusively selling cosmetics",
        24892,
    ),
    MOTORCYCLE(
        "A shop that sells motorcycles and/or related accessories and services.",
        24415,
    ),
    TAILOR(
        "A place where clothing is made, repaired, or altered professionally, especially suits and men's clothing.",
        23511,
    ),
    COPYSHOP(
        "A shop that offers photocopying and printing services.",
        23086,
    ),
    GARDEN_CENTRE(
        "A shop selling potted plants, seedlings for planting, and related items.",
        23068,
    ),
    FUNERAL_DIRECTORS(
        "Providing services related to funeral arrangements, may also be known as a \"funeral parlour\" or \"undertakers\".",
        23046,
    ),
    TOBACCO(
        "A shop selling tobacco, and possibly other convenience items",
        21268,
    ),
    FARM(
        "A shop at a farm, selling farm produce.",
        20488,
    ),
    TOYS(
        "A shop focussed on selling children's toys.",
        19827,
    ),
    DELI(
        "A delicatessen store",
        18702,
    ),
    STORAGE_RENTAL(
        "paid storage of household goods, i.e. self storage",
        18608,
    ),
    TRADE(
        "A place of business that sells to a particular trade or trades, but normally also retails to normal consumers.",
        18325,
    ),
    INTERIOR_DECORATION(
        "Shop focused on selling interior decorations",
        18058,
    ),
    SEAFOOD(
        "A shop selling fish/seafood.",
        17934,
    ),
    MASSAGE(
        "Massage shop",
        17822,
    ),
    TICKET(
        "A shop selling tickets for concerts, events, public transport, ...",
        17288,
    ),
    HOUSEWARE(
        "A shop selling small household items",
        15856,
    ),
    PASTRY(
        "A shop where sweet bakery products are produced and sold",
        15646,
    ),
    PHOTO(
        "A shop dealing with photos or video in any way.",
        14449,
    ),
    WINE(
        "Shop selling wine",
        14312,
    ),
    GENERAL(
        "A general store. Small shop selling variety of different products.",
        13489,
    ),
    OUTDOOR(
        "A shop selling trekking, climbing, camping equipment.",
        13113,
    ),
    PAINT(
        "A shop where you can buy paints.",
        12897,
    ),
    OUTPOST(
        "Shop primarily used to pick-up items ordered online. May have meager supply of products.",
        12354,
    ),
    ART(
        "A shop which sells works of art.",
        12351,
    ),
    BOOKMAKER(
        "A shop that takes bets on sporting and other events at agreed upon odds.",
        12340,
    ),
    CHARITY(
        "A shop operated by a charity",
        12044,
    ),
    PAWNBROKER(
        "A business that offers secured loans against items of personal property as collateral.",
        11902,
    ),
    SECOND_HAND(
        "A shop selling second hand goods",
        11257,
    ),
    TATTOO(
        "A place where people can get permanent tattoos",
        11066,
    ),
    MEDICAL_SUPPLY(
        "A store where you can buy medical equipment for private persons.",
        11057,
    ),
    KITCHEN(
        "A shop where you can plan and buy your kitchen.",
        10889,
    ),
    FABRIC(
        "A shop that sells fabric",
        10788,
    ),
    BOUTIQUE(
        "A small shop that sells expensive or designer clothing and/or accessories.",
        10653,
    ),
    BED(
        "A shop that specialises in selling mattresses and other bedding products.",
        10623,
    ),
    LOTTERY(
        "A shop of which the main or only purpose is the sale of lottery tickets.",
        9966,
    ),
    ANTIQUES(
        "A shop where you can buy antiques",
        9355,
    ),
    WHOLESALE(
        "A store that sells items in bulk.",
        8965,
    ),
    CRAFT(
        "A place where customers can buy supplies for making art and crafts.",
        8488,
    ),
    GAS(
        "A shop selling and/or refilling bottled gas.",
        8234,
    ),
    COFFEE(
        "A shop selling coffee",
        7879,
    ),
    PERFUMERY(
        "A shop selling perfumes.",
        6915,
    ),
    HEARING_AIDS(
        "A shop specialized in selling hearing aids devices",
        6723,
    ),
    TEA(
        "Shop selling tea",
        6654,
    ),
    MUSICAL_INSTRUMENT(
        "Shop selling musical instruments, lyrics, scores.",
        6445,
    ),
    BABY_GOODS(
        "A shop where you can buy objects for babies, like clothes, prams, cots or baby's baths",
        6416,
    ),
    MUSIC(
        "A store that primarily sells recorded music (vinyl/CDs)",
        6296,
    ),
    E_CIGARETTE(
        "A shop selling electronic cigarettes",
        6227,
    ),
    BAG(
        "A shop selling bags.",
        5878,
    ),
    AGRARIAN(
        "Shop selling products for agricultural use, such as pesticides (as in the picture), seeds, animal feed, etc.",
        5848,
    ),
    CARPET(
        "A shop selling carpets.",
        5475,
    ),
    MONEY_LENDER(
        "Shop offering small personal loans at high rates of interest",
        5457,
    ),
    APPLIANCE(
        "Shop for white goods",
        5449,
    ),
    HIFI(
        "Shop selling high fidelity audio components",
        5164,
    ),
    VIDEO_GAMES(
        "Shop selling video games.",
        5083,
    ),
    DAIRY(
        "A shop selling dairy products.",
        5072,
    ),
    ELECTRICAL(
        "Shop selling electrical supplies and devices.",
        4778,
    ),
    MOTORCYCLE_REPAIR(
        "A place where you can get your motorcycles repaired.",
        4686,
    ),
    CHOCOLATE(
        "Shop focused on selling chocolate.",
        4673,
    ),
    CHEESE(
        "A shop mainly selling cheese.",
        4667,
    ),
    VIDEO(
        "A shop that sells or rents out videos/DVDs",
        4487,
    ),
    LOCKSMITH(
        "A shop where you can get keys cut",
        4086,
    ),
    FISHING(
        "Store where you can buy fishing equipment.",
        4034,
    ),
    NUTRITION_SUPPLEMENTS(
        "Shops that sells nutritional supplements different types of purified proteins, fats, vitamins, minerals and/or herbs.",
        4030,
    ),
    GROCERY(
        "A retail store that specializes in selling non-perishable food.",
        3975,
    ),
    ESTATE_AGENT(
        "A shop which sells and/or rents property.",
        3965,
    ),
    HEALTH_FOOD(
        "A health food shop; selling wholefoods, vitamins, nutrition supplements and meat and dairy alternatives.",
        3963,
    ),
    BATHROOM_FURNISHING(
        "A shop selling bathroom furnishings.",
        3928,
    )
}
