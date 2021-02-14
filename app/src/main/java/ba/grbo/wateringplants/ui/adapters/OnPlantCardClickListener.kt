package ba.grbo.wateringplants.ui.adapters

import ba.grbo.wateringplants.data.Plant

class OnPlantCardClickListener(private val onPlantCardClickListener: (Int) -> Unit) {
    fun onClick(plant: Plant) = onPlantCardClickListener(plant.id)
}