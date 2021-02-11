package ba.grbo.wateringplants.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.MutableStateFlow

@Entity(tableName = "plants_table")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val _name: MutableStateFlow<String>,
    val _description: MutableStateFlow<String>,
    @ColumnInfo(name = "watering_period")
    val _wateringPeriod: MutableStateFlow<String>,
    val _image: MutableStateFlow<Image>
) {
    constructor(
        name: MutableStateFlow<String> = MutableStateFlow(""),
        description: MutableStateFlow<String> = MutableStateFlow(""),
        wateringPeriod: MutableStateFlow<String> = MutableStateFlow("1"),
        image: MutableStateFlow<Image> = MutableStateFlow(Image("", 0f))
    ) : this(0, name, description, wateringPeriod, image)

    var name: String
        get() = _name.value
        set(value) {
            _name.value = value
        }

    var description: String
        get() = _description.value
        set(value) {
            _description.value = value
        }

    var wateringPeriod: String
        get() = _wateringPeriod.value
        set(value) {
            _wateringPeriod.value = value
        }

    var image: Image
        get() = _image.value
        set(value) {
            _image.value = when (value.rotationAngle) {
                270f -> Image(value.path, -90f)
                -270f -> Image(value.path, 90f)
                else -> value
            }
        }

    override fun toString() =
        "${javaClass.simpleName}(id=$id, name=$name, description=$description, " +
                "waterinPeriod=$wateringPeriod, image=$image)"

    override fun equals(other: Any?) = when (other) {
        null,
        !is Plant -> false
        else -> id == other.id &&
                name == other.name &&
                description == other.description &&
                wateringPeriod == other.wateringPeriod &&
                image == other.image
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + wateringPeriod.hashCode()
        result = 31 * result + image.hashCode()
        return result
    }
}