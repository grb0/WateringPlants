package ba.grbo.wateringplants.ui.adapters

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.databinding.PlantItemBinding
import ba.grbo.wateringplants.util.RotationTransformation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

class PlantAdapter : ListAdapter<Plant, PlantAdapter.ViewHolder>(PlantDiffCallbacks()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder private constructor(
        private val binding: PlantItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup) = ViewHolder(
                PlantItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        fun bind(plant: Plant) {
            binding.plantName.text = plant.name
            loadAndSetImage(plant)
        }

        private fun loadAndSetImage(plant: Plant) {
            binding.imgLoadingProgress.visibility = View.VISIBLE
            Glide.with(binding.plantImg.context)
                .load(Uri.fromFile(File(plant.image.path)))
                .transform(RotationTransformation(plant.image.rotationAngle))
                .listener(provideRequestListener())
                .into(binding.plantImg)
        }

        private fun provideRequestListener() = object : RequestListener<Drawable?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>?,
                isFirstResource: Boolean
            ): Boolean {
                binding.imgLoadingProgress.visibility = View.GONE
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable?>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                binding.imgLoadingProgress.visibility = View.GONE
                return false
            }
        }
    }
}