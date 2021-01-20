package ba.grbo.wateringplants.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ba.grbo.wateringplants.databinding.FragmentPlantsBinding

class PlantsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPlantsBinding.inflate(inflater, container, false).root
    }
}