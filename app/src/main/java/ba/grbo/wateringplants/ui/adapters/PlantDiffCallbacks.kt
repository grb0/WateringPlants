package ba.grbo.wateringplants.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import ba.grbo.wateringplants.data.Plant

class PlantDiffCallbacks : DiffUtil.ItemCallback<Plant>() {
    override fun areItemsTheSame(oldItem: Plant, newItem: Plant): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Plant, newItem: Plant): Boolean {
        return oldItem == newItem
    }
}