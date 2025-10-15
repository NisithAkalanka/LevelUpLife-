package com.example.leveluplife.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leveluplife.R
import com.google.android.material.textfield.TextInputEditText

class AddQuestFragment : Fragment() {

    private val viewModel: QuestsViewModel by activityViewModels()

    // Voice-to-Quest launchers
    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val data = res.data ?: return@registerForActivityResult
            val results =
                data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) ?: return@registerForActivityResult
            val text = results.firstOrNull()?.trim().orEmpty()
            view?.findViewById<TextInputEditText>(R.id.et_custom_quest)?.setText(text)
        }

    private val micPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startVoice()
            else Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
        }

    private fun startVoice() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }
        voiceLauncher.launch(i)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_add_quest, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI refs
        val toolbar: Toolbar = view.findViewById(R.id.toolbar_add_quest)
        val suggestionsRv: RecyclerView = view.findViewById(R.id.suggestions_recycler_view)
        val etCustom: TextInputEditText = view.findViewById(R.id.et_custom_quest)
        val addBtn: Button = view.findViewById(R.id.btnAddQuest) // <- FIXED id
        val micBtn: ImageButton? = view.findViewById(R.id.btnMic)

        // Back
        toolbar.setNavigationOnClickListener { activity?.supportFragmentManager?.popBackStack() }

        // Suggestions
        val suggestions = listOf(
            "Walk 5,000 steps",
            "Read a chapter of a book",
            "Meditate for 10 minutes",
            "Drink 8 glasses of water",
            "Learn something new for 15 minutes",
            "Plan your next day"
        )
        suggestionsRv.layoutManager = LinearLayoutManager(requireContext())
        suggestionsRv.adapter = SuggestionsAdapter(suggestions) { picked ->
            viewModel.addNewQuest(picked)
            Toast.makeText(context, "'$picked' added!", Toast.LENGTH_SHORT).show()
            activity?.supportFragmentManager?.popBackStack()
        }

        // Add custom quest
        addBtn.setOnClickListener {
            val title = etCustom.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                Toast.makeText(context, "Please enter a quest title.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addNewQuest(title)
                Toast.makeText(context, "'$title' added!", Toast.LENGTH_SHORT).show()
                activity?.supportFragmentManager?.popBackStack()
            }
        }

        // Mic
        micBtn?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) startVoice()
            else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // --- RecyclerView adapter using a custom row item ---
    private class SuggestionsAdapter(
        private val items: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SuggestionsAdapter.VH>() {

        inner class VH(val root: View) : RecyclerView.ViewHolder(root) {
            private val tv = root.findViewById<android.widget.TextView>(R.id.tvSuggestion)
            fun bind(text: String) {
                tv.text = text
                root.setOnClickListener { onClick(text) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_suggestion, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount(): Int = items.size
    }
}