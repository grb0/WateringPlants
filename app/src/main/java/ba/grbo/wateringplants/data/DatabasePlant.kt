package ba.grbo.wateringplants.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "plants_table")
data class DatabasePlant(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val description: String,
    @ColumnInfo(name = "watering_period")
    val wateringPeriod: String,
    val imagePath: String
) {
    @Ignore
    val imageUri = Uri.fromFile(File(imagePath))

    constructor(
        name: String,
        description: String,
        wateringPeriod: String,
        imagePath: String
    ) : this(0, name, description, wateringPeriod, imagePath)
}