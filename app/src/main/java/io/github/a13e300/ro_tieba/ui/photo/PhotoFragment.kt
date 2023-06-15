package io.github.a13e300.ro_tieba.ui.photo

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.zoom.SketchZoomImageView
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.PhotoUtils
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.StatusBarConfig
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.FragmentPhotoBinding
import io.github.a13e300.ro_tieba.forceShowIcon
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import kotlinx.coroutines.launch

private const val KEY_FULLSCREEN = "fullscreen"

class PhotoFragment : BaseFragment() {
    private var isFullscreen = false

    private val viewModel: PhotoViewModel by viewModels({ findNavController().previousBackStackEntry!! })
    private lateinit var binding: FragmentPhotoBinding
    private var oldIsAppearanceLightStatusBars = false

    override fun onStart() {
        super.onStart()
        oldIsAppearanceLightStatusBars = insetsController.isAppearanceLightStatusBars
        insetsController.isAppearanceLightStatusBars = false
    }

    override fun onStop() {
        super.onStop()
        insetsController.isAppearanceLightStatusBars = oldIsAppearanceLightStatusBars
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        savedInstanceState?.getBoolean(KEY_FULLSCREEN)?.let { isFullscreen = it }
        viewModel.currentIndex.observe(viewLifecycleOwner) {
            binding.toolbar.title = "${it + 1} / ${viewModel.photos.size}"
        }
        binding.photoPager.apply {
            adapter = PhotoAdapter(viewModel.photos)
            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.currentIndex.value = position
                    val photo = viewModel.photos[position]
                    val content = when (val s = photo.source) {
                        is Post -> s.content
                        is TiebaThread -> s.content
                        else -> null
                    }
                    if (content != null) {
                        // FIXME: sometimes the textview only shows 1 line
                        binding.imageText.text =
                            SpannableStringBuilder().appendSimpleContent(content, requireContext())
                    } else {
                        binding.imageText.text = null
                    }
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
                return onContextItemSelected(menuItem)
            }

        })
        setupToolbar(binding.toolbar)
        return binding.root
    }

    override fun onInitStatusBar(): StatusBarConfig {
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        return StatusBarConfig(false, !isFullscreen)
    }

    private fun updateFullscreen() {
        if (isFullscreen) {
            binding.appBar.isVisible = false
            binding.photoBottomBar.isVisible = false
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            binding.appBar.isVisible = true
            binding.photoBottomBar.isVisible = true
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FULLSCREEN, isFullscreen)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        MenuInflater(requireContext()).inflate(R.menu.post_item_menu, menu)
        menu.forceShowIcon()
        menu.setGroupVisible(R.id.group_post, false)
        menu.setGroupVisible(R.id.group_link, false)
        menu.setGroupVisible(R.id.group_photo, true)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.download_photo, R.id.save_photo -> {
                val photo = viewModel.photos[viewModel.currentIndex.value!!]
                lifecycleScope.launch {
                    PhotoUtils.downloadPhoto(
                        activity = requireActivity(),
                        photo = photo,
                        onSuccess = {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.saved_to_gallery),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = {
                            Snackbar.make(
                                binding.root,
                                "error:${it.message}",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                true
            }

            R.id.share_photo -> {
                lifecycleScope.launch {
                    PhotoUtils.sharePhoto(
                        context = requireContext(),
                        photo = viewModel.photos[viewModel.currentIndex.value!!],
                        onFailure = {
                            Snackbar.make(
                                binding.root,
                                "failed to share:${it.message}",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                true
            }

            else -> false
        }
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

                setOnClickListener {
                    isFullscreen = !isFullscreen
                    updateFullscreen()
                }
                registerForContextMenu(this)
            })
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.imageView.displayImage(items[position].url)
        }
    }

}