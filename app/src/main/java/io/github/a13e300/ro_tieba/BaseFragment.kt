package io.github.a13e300.ro_tieba

import android.content.Intent
import android.net.Uri
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.IPost
import io.github.a13e300.ro_tieba.models.Photo
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.utils.PhotoUtils
import io.github.a13e300.ro_tieba.utils.copyText
import io.github.a13e300.ro_tieba.utils.forceShowIcon
import io.github.a13e300.ro_tieba.utils.openPostAtOtherClient
import io.github.a13e300.ro_tieba.view.ItemView
import io.github.a13e300.ro_tieba.view.SelectedLink
import io.github.a13e300.ro_tieba.view.SelectedUser
import kotlinx.coroutines.launch

data class StatusBarConfig(
    val light: Boolean,
    val show: Boolean
)

abstract class BaseFragment : Fragment() {
    protected lateinit var insetsController: WindowInsetsControllerCompat
    private var mEntry: NavBackStackEntry? = null
    override fun onStart() {
        super.onStart()
        insetsController = (requireActivity() as BaseActivity).insetsController
        mEntry = findNavController().currentBackStackEntry
        val config = onInitStatusBar()
        if (config.show)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        else
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.isAppearanceLightStatusBars = config.light
    }

    protected fun setupToolbar(toolbar: Toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationContentDescription(R.string.back_title)
        toolbar.setNavigationOnClickListener {
            navigateUp()
        }
    }

    fun navigateUp() {
        // we don't like navigate up to home
        if (mEntry == null || isRemoving || isDetached || !isAdded || activity == null) return
        val navController = findNavController()
        if (mEntry != navController.currentBackStackEntry) return
        if (!navController.popBackStack()) {
            activity?.finish()
        }
    }

    protected open fun onInitStatusBar(): StatusBarConfig {
        val ta =
            requireContext().obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.isLightTheme))
        val isLight = ta.getBoolean(0, false)
        ta.recycle()
        return StatusBarConfig(
            light = isLight,
            show = true
        )
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        MenuInflater(requireContext()).inflate(R.menu.post_item_menu, menu)
        menu.forceShowIcon()
        val selected = (menuInfo as? ItemView.ContextMenuInfo)?.selectedData
        if (selected is SelectedLink) {
            menu.setGroupVisible(R.id.group_link, true)
        }
        if (selected is Photo) {
            menu.setGroupVisible(R.id.group_photo, true)
        }
        if (selected is SelectedUser) {
            menu.setGroupVisible(R.id.group_user, true)
        }
        ((menuInfo as? ItemView.ContextMenuInfo)?.data as? IPost)?.content?.find { it is Content.VideoContent }
            ?.let { menu.setGroupVisible(R.id.group_video, true) }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? ItemView.ContextMenuInfo
        val selected = info?.selectedData
        val post = if (selected is Comment) selected else info?.data
        when (item.itemId) {
            R.id.copy_post_content -> {
                val content = when (post) {
                    is TiebaThread -> post.content
                    is Post -> post.content
                    is Comment -> post.content
                    else -> return false
                }
                val text = content.joinToString("") {
                    when (it) {
                        is Content.TextContent -> it.text
                        is Content.ImageContent -> "[${it.src}]"
                        is Content.LinkContent -> "[${it.text}](${it.link})"
                        is Content.EmojiContent -> Emotions.emotionMap.get(it.id)?.name ?: it.id
                        is Content.VideoContent -> "[video](${it.src})"
                        is Content.UserContent -> it.text
                        is Content.UnknownContent -> it.source
                    }
                }
                if (post is TiebaThread) copyText("${post.title}\n$text")
                else copyText(text)
                return true
            }

            R.id.copy_post_link -> {
                val text = when (post) {
                    is TiebaThread -> "https://tieba.baidu.com/p/${post.tid}?pid=${post.postId}"
                    is Post -> "https://tieba.baidu.com/p/${post.tid}?pid=${post.postId}"
                    is Comment -> "https://tieba.baidu.com/p/${post.tid}?pid=${post.postId}&cid=${post.ppid}"
                    else -> ""
                }
                copyText(text)
                return true
            }

            R.id.open_at_other_client -> {
                val tid = when (post) {
                    is Post -> post.tid
                    is Comment -> post.tid
                    is TiebaThread -> post.tid
                    else -> return false
                }
                val pid = when (post) {
                    is Post -> post.postId
                    is Comment -> post.postId
                    is TiebaThread -> 0L
                    else -> return false
                }
                if (!openPostAtOtherClient(tid, pid, requireContext())) {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.no_other_apps_tips),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                return true
            }

            R.id.open_link -> {
                (selected as? SelectedLink)?.url?.also {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                        bundleOf(EXTRA_DONT_USE_NAV to true)
                    )
                }
                return true
            }

            R.id.copy_link -> {
                (selected as? SelectedLink)?.url?.also {
                    copyText(it)
                }
                return true
            }

            R.id.save_photo -> {
                (selected as? Photo)?.let { photo ->
                    lifecycleScope.launch {
                        PhotoUtils.downloadPhoto(
                            activity = requireActivity(),
                            photo = photo,
                            onSuccess = {
                                Snackbar.make(
                                    requireView(),
                                    getString(R.string.saved_to_gallery),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = {
                                Snackbar.make(
                                    requireView(),
                                    "error:${it.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
                return true
            }

            R.id.share_photo -> {
                lifecycleScope.launch {
                    (selected as? Photo)?.let { photo ->
                        PhotoUtils.sharePhoto(
                            context = requireContext(),
                            photo = photo,
                            onFailure = {
                                Snackbar.make(
                                    requireView(),
                                    "failed to share:${it.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
                return true
            }

            R.id.save_video -> {
                val video =
                    (post as? Post)?.content?.find { it is Content.VideoContent } as? Content.VideoContent
                video?.src?.let {
                    lifecycleScope.launch {
                        PhotoUtils.downloadVideo(
                            activity = requireActivity(),
                            url = it,
                            post = post,
                            onSuccess = {
                                Snackbar.make(
                                    requireView(),
                                    getString(R.string.saved_to_gallery),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = {
                                Snackbar.make(
                                    requireView(),
                                    "error:${it.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
                return true
            }

            R.id.open_video -> {
                val video =
                    (selected as? Post)?.content?.find { it is Content.VideoContent } as? Content.VideoContent
                video?.text?.let {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                        bundleOf(EXTRA_DONT_USE_NAV to true)
                    )
                }
                return true
            }

            R.id.copy_video_link -> {
                val video =
                    (selected as? Post)?.content?.find { it is Content.VideoContent } as? Content.VideoContent
                video?.src?.also {
                    copyText(it)
                }
                return true
            }

            R.id.open_profile -> {
                (selected as? SelectedUser)?.let {
                    findNavController().navigate(MobileNavigationDirections.showProfile(it.uid.toString()))
                }
                return true
            }
        }
        return super.onContextItemSelected(item)
    }
}