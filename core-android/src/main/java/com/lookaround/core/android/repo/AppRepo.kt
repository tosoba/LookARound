package com.lookaround.core.android.repo

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationRequest
import com.jintin.fancylocation.LocationData
import com.jintin.fancylocation.LocationFlow
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.repo.IAppRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.beryukhov.reactivenetwork.ReactiveNetwork
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class AppRepo
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val reactiveNetwork: ReactiveNetwork
) : IAppRepo {
    override val isConnectedFlow: Flow<Boolean>
        @SuppressLint("MissingPermission")
        get() = reactiveNetwork.observeNetworkConnectivity(context).map { it.available }

    override val isLocationAvailable: Boolean
        get() =
            LocationManagerCompat.isLocationEnabled(
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            )

    override val locationDataFlow: Flow<LocationDataDTO>
        @SuppressLint("MissingPermission")
        get() {
            val locationRequest =
                LocationRequest.create()
                    .setInterval(3000L)
                    .setFastestInterval(3000L)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            return LocationFlow(context, locationRequest).get().map { locationData ->
                when (locationData) {
                    is LocationData.Success ->
                        LocationDataDTO.Success(
                            locationData.location.latitude,
                            locationData.location.longitude,
                            locationData.location.altitude
                        )
                    is LocationData.Fail -> LocationDataDTO.Failure
                }
            }
        }
}
