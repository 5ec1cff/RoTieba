package io.github.a13e300.ro_tieba.ui.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.zoom.SketchZoomImageView
import io.github.a13e300.ro_tieba.databinding.FragmentPhotoBinding

class PhotoFragment : Fragment() {

    private val viewModel: PhotoViewModel by viewModels({ findNavController().previousBackStackEntry!! })
    private lateinit var windowInsetsControllerCompat: WindowInsetsControllerCompat
    private var oldIsAppearanceLightStatusBars = false

    override fun onStart() {
        super.onStart()
        oldIsAppearanceLightStatusBars = windowInsetsControllerCompat.isAppearanceLightStatusBars
        windowInsetsControllerCompat.isAppearanceLightStatusBars = false
    }

    override fun onStop() {
        super.onStop()
        windowInsetsControllerCompat.isAppearanceLightStatusBars = oldIsAppearanceLightStatusBars
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPhotoBinding.inflate(inflater, container, false)
        viewModel.currentIndex.observe(viewLifecycleOwner) {
            binding.toolbar.title = "${it + 1} / ${viewModel.photos.size}"
        }
        binding.photoPager.apply {
            adapter = PhotoAdapter(viewModel.photos)
            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.currentIndex.value = position
                }
            })
            viewModel.currentIndex.value?.let {
                setCurrentItem(it, false)
            }
        }
        windowInsetsControllerCompat =
            WindowCompat.getInsetsController(requireActivity().window, binding.root)
        return binding.root
    }

    class PhotoViewHolder(val imageView: SketchZoomImageView) : RecyclerView.ViewHolder(imageView)

    inner class PhotoAdapter(private val items: List<Photo>) :
        RecyclerView.Adapter<PhotoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            return PhotoViewHolder(SketchZoomImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.imageView.displayImage(items[position].url)
        }
    }

}