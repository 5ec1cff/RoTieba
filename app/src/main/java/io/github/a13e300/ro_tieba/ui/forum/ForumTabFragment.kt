package io.github.a13e300.ro_tieba.ui.forum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.github.a13e300.ro_tieba.databinding.FragmentForumTabBinding
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel

class ForumTabFragment : Fragment() {
    private val viewModel: ForumTabViewModel by viewModels()
    private val parentViewModel: ForumViewModel by viewModels({ requireParentFragment() })
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private lateinit var binding: FragmentForumTabBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForumTabBinding.inflate(inflater, container, false)

        return binding.root
    }
}