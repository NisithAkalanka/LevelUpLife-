package com.example.leveluplife.ui
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.leveluplife.R
import com.example.leveluplife.data.repository.QuestRepository

class AchievementsFragment: Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View? =
        i.inflate(R.layout.fragment_achievements, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        val repo = QuestRepository(requireContext())
        v.findViewById<TextView>(R.id.tvStreak).text = "Current streak: ${repo.getStreak()} days"
        val container = v.findViewById<LinearLayout>(R.id.badgeContainer)
        val badges = repo.getBadges()
        if (badges.isEmpty()) {
            container.addView(TextView(requireContext()).apply { text = "Badges නැහැ. අදේවෙන් ඉදිරියට! 💪" })
        } else {
            badges.forEach {
                val tv = layoutInflater.inflate(R.layout.item_badge, container, false) as TextView
                tv.text = it
                container.addView(tv)
            }
        }
    }
}