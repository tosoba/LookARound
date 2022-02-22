package com.lookaround.core.android.model

import android.os.Parcelable
import com.lookaround.core.model.IPlaceType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Sport(
    override val description: String,
    override val count: Int,
) : IPlaceType, Parcelable {
    SOCCER(
        "Association football, more commonly known as football or soccer",
        499420,
    ),
    TENNIS(
        "A competition where two or four players use a ball and rackets over a net.",
        413369,
    ),
    BASKETBALL(
        "A sport played by two teams of five players on a rectangular court.",
        206641,
    ),
    BASEBALL(
        "A bat-and-ball game played between two teams of nine players on a field (pitch or stadium) each who take turns batting and fielding.",
        148679,
    ),
    MULTI(
        "Property added to otherwise marked sports facility to indicate as suitable for more than one sport, without listing them",
        121547,
    ),
    SWIMMING(
        "A place where people do swimming",
        92552,
    ),
    GOLF(
        "Golf",
        72236,
    ),
    EQUESTRIAN(
        "A sport practised with the horse as a partner; but for horse racing use tag:sport=horse_racing.",
        62062,
    ),
    RUNNING(
        "Features dedicated to the sport of running",
        39912,
    ),
    FITNESS(
        "Fitness sports",
        34635,
    ),
    ATHLETICS(
        "A collection of sports which combines various athletic contests based on the skills of running, jumping, and throwing.",
        30841,
    ),
    TABLE_TENNIS(
        "A bat and ball game played over a table.",
        26362,
    ),
    BEACHVOLLEYBALL(
        "Volleyball played on a sand court",
        25038,
    ),
    CLIMBING(
        "Marks elements to represent natural climbing sites (climbing areas, sectors, crags, frozen waterfalls, etc.) or artificial climbs (climbing walls, indoor climbing halls, etc.)",
        24432,
    ),
    VOLLEYBALL(
        "Volleyball",
        23529,
    ),
    SKATEBOARD(
        "An area designated and equipped for skateboarding",
        20995,
    ),
    BOULES(
        "A group of games in which the objective is to throw or roll heavy balls as close as possible to a small target ball, also called pétanque, lyonnaise, bocce, or bocce volo.",
        20310,
    ),
    AMERICAN_FOOTBALL(
        "A sport played by two teams of eleven players on a rectangular field with goalposts at each end.",
        18869,
    ),
    MOTOR(
        "Motorsport: a sport with motorised vehicles, e.g. auto racing",
        16411,
    ),
    BOWLS(
        "Sport in which the objective is to roll biased balls so that they stop close to a smaller ball",
        15601,
    ),
    SHOOTING(
        "Identifies given object as used for practicing shooting sports",
        13184,
    ),
    CRICKET(
        "A bat-and-ball sport contested by two teams, usually of eleven players, each on a large grass Cricket pitch. Played on a large circular or oval-shaped grassy Cricket field ground.",
        12778,
    ),
    NETBALL(
        "A hand ball competition between two teams on a rectangular court.",
        10570,
    ),
    FUTSAL(
        "Futsal is a sport played by two teams of five players on a rectangular court.",
        6626,
    ),
    HORSE_RACING(
        "An equestrian sport in which several horses simultaneously race against each other.",
        6197,
    ),
    CYCLING(
        "The use of bicycles for sport, also called bicycling, mountain biking or biking.",
        5971,
    ),
    RUGBY_UNION(
        "Rugby union football",
        5944,
    ),
    MOTOCROSS(
        "Motorcycle racing on unpaved surfaces.",
        5885,
    ),
    SKIING(
        "Identifies given object as used for practicing skiing",
        5879,
    ),
    GYMNASTICS(
        "Gymnastics",
        5809,
    ),
    KARTING(
        "Kart racing",
        5555,
    ),
    HANDBALL(
        "A team sport played with goals and a thrown ball using the hands.",
        4784,
    ),
    FREE_FLYING(
        "Provides a way to tag landing and takeoff for free flying aircraft with additional related amenities.",
        4700,
    ),
    BADMINTON(
        "A racquet sport played by singles or in teams of two, who take positions on opposite halves of a rectangular court (pitch) divided by a net.",
        4612,
    ),
    PADEL(
        "A racket sport played in pairs. It consists of bouncing the ball in the opponent's court, with the possibility of bouncing it off the walls.",
        4538,
    ),
    SOFTBALL(
        "A bat-and-ball game similar to baseball.",
        4480,
    ),
    ICE_HOCKEY(
        "Ice hockey ring.",
        4016,
    ),
    FIELD_HOCKEY(
        "Field Hockey",
        3968,
    ),
    SCUBA_DIVING(
        "To mark a physical object as for scuba diving",
        3894,
    ),
    CHESS(
        "Chess is a popular two-player strategy board game.",
        3859,
    ),
    CANOE(
        "Canoe and Kayak.",
        3725,
    ),
    YOGA(
        "Yoga as exercise",
        3517,
    ),
    ARCHERY(
        "The art, practice, or skill of propelling arrows with the use of a bow.",
        3363,
    ),
}
