package ba.grbo.wateringplants.data.source.local

import androidx.room.TypeConverter
import ba.grbo.wateringplants.data.Image
import kotlinx.coroutines.flow.MutableStateFlow

class Converter {
    @TypeConverter
    fun sharedFlowStringToString(value: MutableStateFlow<String>) = value.value

    @TypeConverter
    fun stringToSharedFlowString(value: String) = MutableStateFlow(value)

    @TypeConverter
    fun sharedFlowImageToString(value: MutableStateFlow<Image>) =
        "${value.value.path},${value.value.rotationAngle}"

    @TypeConverter
    fun stringToSharedFlowImage(value: String): MutableStateFlow<Image> {
        val image = value.split(',')
        return MutableStateFlow(Image(image[0], image[1].toFloat()))
    }
}