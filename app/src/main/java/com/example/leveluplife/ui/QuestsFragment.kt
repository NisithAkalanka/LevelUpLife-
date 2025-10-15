package com.example.leveluplife.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leveluplife.R
import com.example.leveluplife.data.Quest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class QuestsFragment : Fragment() {

    private val vm: QuestsViewModel by activityViewModels()
    private lateinit var adapter: QuestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_quests, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load()

        // Back button
        view.findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        val rv = view.findViewById<RecyclerView>(R.id.rvQuests)
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)

        adapter = QuestsAdapter(
            onChecked = { q, isChecked ->
                vm.toggleQuest(q.id)
                safeUpdateStreak()
                rv.post { adapter.submitList(snapshotFiltered(etSearch.text?.toString())) }

                val msg = if (isChecked) "Completed '${q.title}'" else "Marked '${q.title}' active"
                Snackbar.make(rv, msg, Snackbar.LENGTH_SHORT)
                    .setAction("Undo") {
                        vm.toggleQuest(q.id)
                        rv.post { adapter.submitList(snapshotFiltered(etSearch.text?.toString())) }
                    }.show()
            },
            onDelete = { q -> deleteWithUndo(rv, q) },
            onEdit   = { q -> showRenameDialog(rv, q) }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        adapter.submitList(vm.questList.toList())

        // Search
        etSearch.addTextChangedListener { text ->
            rv.post { adapter.submitList(snapshotFiltered(text?.toString())) }
        }

        // Swipe: LEFT = delete, RIGHT = toggle
        val helper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(pos) ?: return
                if (dir == ItemTouchHelper.LEFT) {
                    deleteWithUndo(rv, item)
                } else {
                    vm.toggleQuest(item.id)
                    safeUpdateStreak()
                    rv.post { adapter.submitList(snapshotFiltered(etSearch.text?.toString())) }
                    Snackbar.make(rv, "Toggled '${item.title}'", Snackbar.LENGTH_SHORT)
                        .setAction("Undo") {
                            vm.toggleQuest(item.id)
                            rv.post { adapter.submitList(snapshotFiltered(etSearch.text?.toString())) }
                        }.show()
                }
            }
        })
        helper.attachToRecyclerView(rv)

        fab.setOnClickListener {
            (activity as? com.example.leveluplife.MainActivity)?.navigateTo(AddQuestFragment())
        }
    }

    /** Undoable delete */
    private fun deleteWithUndo(rv: RecyclerView, item: Quest) {
        vm.deleteQuest(item.id)
        rv.post { adapter.submitList(vm.questList.toList()) }
        Snackbar.make(rv, "Deleted '${item.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                vm.addNewQuest(item.title)
                rv.post { adapter.submitList(vm.questList.toList()) }
            }.show()
    }

    /** Search helper */
    private fun snapshotFiltered(qText: String?): List<Quest> {
        val q = qText?.trim().orEmpty().lowercase()
        val base = vm.questList
        return if (q.isEmpty()) base.toList()
        else base.filter { it.title.lowercase().contains(q) }.toList()
    }

    /** Rename dialog */
    private fun showRenameDialog(rv: RecyclerView, q: Quest) {
        val input = EditText(requireContext()).apply {
            setText(q.title)
            setSelection(text.length)
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename quest")
            .setView(input)
            .setPositiveButton("Save") { d, _ ->
                val newTitle = input.text?.toString()?.trim().orEmpty()
                if (newTitle.isNotEmpty() && newTitle != q.title) {
                    // simple replace by delete+add
                    vm.deleteQuest(q.id)
                    vm.addNewQuest(newTitle)
                    rv.post { adapter.submitList(snapshotFiltered(null)) }
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        // keyboard up
        requireContext().getSystemService<InputMethodManager>()
            ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    /** If ViewModel has TODO() inside updateStreakAndBadges, don't crash */
    private fun safeUpdateStreak() {
        try { vm.updateStreakAndBadges() } catch (_: NotImplementedError) { /* ignore for now */ }
    }
}