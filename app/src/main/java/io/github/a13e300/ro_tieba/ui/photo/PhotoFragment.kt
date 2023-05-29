package io.github.a13e300.ro_tieba.ui.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import com.github.panpf.sketch.zoom.SketchZoomImageView
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.StorageUtils
import io.github.a13e300.ro_tieba.databinding.FragmentPhotoBinding
import io.github.a13e300.ro_tieba.guessExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream

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
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.photo_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.download_photo -> {
                        if (StorageUtils.verifyStoragePermissions(requireActivity())) {
                            lifecycleScope.launch {
                                val photo = viewModel.photos[viewModel.currentIndex.value!!]
                                val result = DownloadRequest(
                                    requireContext(),
                                    photo.url
                                )
                                    .execute()
                                if (result is DownloadResult.Success) {
                                    kotlin.runCatching {
                                        withContext(Dispatchers.IO) {
                                            BufferedInputStream(result.data.data.newInputStream()).use { inputStream ->
                                                val ext = inputStream.guessExtension()
                                                StorageUtils.saveImage(
                                                    "${photo.key}_${System.currentTimeMillis()}.$ext",
                                                    requireContext(),
                                                    inputStream
                                                )
                                            }
                                        }
                                    }.onSuccess {
                                        Snackbar.make(
                                            binding.root,
                                            getString(R.string.saved_to_gallery),
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }.onFailure {
                                        Snackbar.make(
                                            binding.root,
                                            "error:${it.message}",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                } else if (result is DownloadResult.Error) {
                                    Snackbar.make(
                                        binding.root,
                                        "error:${result.throwable.message}",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        true
                    }

                    else -> false
                }
            }

        })
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