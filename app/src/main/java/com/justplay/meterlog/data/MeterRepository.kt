package com.justplay.meterlog.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MeterRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        runCatching {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }

    fun observeBuildings(uid: String): Flow<List<Building>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(uid)
            .collection("buildings")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map(::toBuilding))
            }
        awaitClose { registration.remove() }
    }

    fun observeBuilding(uid: String, buildingId: String): Flow<Building?> = callbackFlow {
        val registration = buildingDocument(uid, buildingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.let(::toBuilding))
            }
        awaitClose { registration.remove() }
    }

    fun observeBuildingMeters(uid: String, buildingId: String): Flow<List<Meter>> = callbackFlow {
        val registration = metersCollection(uid, buildingId)
            .orderBy("floorNumber", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val meters = snapshot?.documents.orEmpty()
                    .map { toMeter(it, buildingId) }
                    .sortedWith(compareBy<Meter> { it.floorNumber }.thenBy { it.name })
                trySend(meters)
            }
        awaitClose { registration.remove() }
    }

    fun observeBuildingMeter(uid: String, buildingId: String, meterId: String): Flow<Meter?> = callbackFlow {
        val registration = metersCollection(uid, buildingId)
            .document(meterId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.let { toMeter(it, buildingId) })
            }
        awaitClose { registration.remove() }
    }

    suspend fun createBuildingWithMeters(
        uid: String,
        name: String,
        floorCount: Int,
        waterCountsByFloor: Map<Int, Int>,
        electricCountsByFloor: Map<Int, Int>
    ): String {
        val now = Timestamp.now()
        val buildingRef = firestore.collection("users")
            .document(uid)
            .collection("buildings")
            .document()

        firestore.runBatch { batch ->
            batch.set(
                buildingRef,
                mapOf(
                    "name" to name.trim(),
                    "floorCount" to floorCount,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
            (1..floorCount).forEach { floor ->
                val waterCount = waterCountsByFloor[floor] ?: 0
                val electricCount = electricCountsByFloor[floor] ?: 0
                (1..waterCount).forEach { index ->
                    val meterRef = buildingRef.collection("meters").document()
                    batch.set(meterRef, createMeterPayload(buildingRef.id, "${floor}F-W$index", MeterType.WATER, floor, now))
                }
                (1..electricCount).forEach { index ->
                    val meterRef = buildingRef.collection("meters").document()
                    batch.set(meterRef, createMeterPayload(buildingRef.id, "${floor}F-E$index", MeterType.ELECTRIC, floor, now))
                }
            }
        }.await()
        return buildingRef.id
    }

    private fun createMeterPayload(
        buildingId: String,
        name: String,
        type: MeterType,
        floorNumber: Int,
        now: Timestamp
    ): Map<String, Any> {
        return mapOf(
            "buildingId" to buildingId,
            "name" to name,
            "type" to type.name,
            "floorNumber" to floorNumber,
            "note" to "",
            "createdAt" to now,
            "updatedAt" to now
        )
    }

    suspend fun updateBuildingMeter(uid: String, buildingId: String, meter: Meter) {
        val payload = mapOf(
            "buildingId" to buildingId,
            "name" to meter.name.trim(),
            "type" to meter.type.name,
            "floorNumber" to meter.floorNumber,
            "note" to meter.note.trim(),
            "createdAt" to (meter.createdAt ?: Timestamp.now()),
            "updatedAt" to Timestamp.now()
        )
        metersCollection(uid, buildingId).document(meter.id).set(payload).await()
    }

    suspend fun addBuildingReading(
        uid: String,
        buildingId: String,
        meterId: String,
        value: Double,
        recordedAt: Timestamp,
        note: String
    ) {
        val payload = mapOf(
            "value" to value,
            "recordedAt" to recordedAt,
            "note" to note.trim(),
            "createdAt" to Timestamp.now()
        )
        metersCollection(uid, buildingId)
            .document(meterId)
            .collection("readings")
            .add(payload)
            .await()
    }

    fun observeBuildingReadings(uid: String, buildingId: String, meterId: String): Flow<List<Reading>> = callbackFlow {
        val registration = metersCollection(uid, buildingId)
            .document(meterId)
            .collection("readings")
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map(::toReading))
            }
        awaitClose { registration.remove() }
    }

    private fun buildingDocument(uid: String, buildingId: String) =
        firestore.collection("users").document(uid).collection("buildings").document(buildingId)

    private fun metersCollection(uid: String, buildingId: String) =
        buildingDocument(uid, buildingId).collection("meters")

    private fun toBuilding(document: DocumentSnapshot): Building {
        return Building(
            id = document.id,
            name = document.getString("name").orEmpty(),
            floorCount = document.getLong("floorCount")?.toInt() ?: 1,
            createdAt = document.getTimestamp("createdAt"),
            updatedAt = document.getTimestamp("updatedAt")
        )
    }

    private fun toMeter(document: DocumentSnapshot, buildingId: String): Meter {
        val floorNumber = document.getLong("floorNumber")?.toInt() ?: 1
        return Meter(
            id = document.id,
            buildingId = document.getString("buildingId") ?: buildingId,
            name = document.getString("name").orEmpty(),
            type = MeterType.fromName(document.getString("type")),
            floorNumber = floorNumber,
            locationLabel = document.getString("locationLabel") ?: "${floorNumber}F",
            note = document.getString("note").orEmpty(),
            createdAt = document.getTimestamp("createdAt"),
            updatedAt = document.getTimestamp("updatedAt")
        )
    }

    private fun toReading(document: DocumentSnapshot): Reading {
        return Reading(
            id = document.id,
            value = document.getDouble("value") ?: 0.0,
            recordedAt = document.getTimestamp("recordedAt"),
            note = document.getString("note").orEmpty(),
            createdAt = document.getTimestamp("createdAt")
        )
    }

    // Compatibility methods for older screens that are no longer the main flow.
    fun observeMeters(uid: String): Flow<List<Meter>> = callbackFlow {
        trySend(emptyList())
        awaitClose {}
    }

    fun observeMeter(uid: String, meterId: String): Flow<Meter?> = callbackFlow {
        trySend(null)
        awaitClose {}
    }

    suspend fun upsertMeter(uid: String, meter: Meter): String = meter.id

    fun observeReadings(uid: String, meterId: String): Flow<List<Reading>> = callbackFlow {
        trySend(emptyList())
        awaitClose {}
    }

    suspend fun addReading(uid: String, meterId: String, value: Double, recordedAt: Timestamp, note: String) = Unit
}
