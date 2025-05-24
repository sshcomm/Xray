package io.github.megasoheilsh.xray.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.megasoheilsh.xray.R
import io.github.megasoheilsh.xray.Settings
import io.github.megasoheilsh.xray.dto.ProfileList
import io.github.megasoheilsh.xray.helper.ProfileTouchHelper
import io.github.megasoheilsh.xray.viewmodel.ProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProfileAdapter(
    private val scope: CoroutineScope,
    private val settings: Settings,
    private val profileViewModel: ProfileViewModel,
    private val profiles: ArrayList<ProfileList>,
    private val profileSelect: (index: Int, profile: ProfileList) -> Unit,
    private val profileEdit: (profile: ProfileList) -> Unit,
    private val profileDelete: (profile: ProfileList) -> Unit,
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>(), ProfileTouchHelper.ProfileTouchCallback {

    override fun onCreateViewHolder(container: ViewGroup, type: Int): ViewHolder {
        val linearLayout = LinearLayout(container.context)
        val item: View = LayoutInflater.from(container.context).inflate(R.layout.item_recycler_main, linearLayout, false)
        return ViewHolder(item)
    }

    override fun getItemCount(): Int {
        return profiles.size
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val profile = profiles[index]
        val color = if (settings.selectedProfile == profile.id) R.color.primaryColor else R.color.btnColor
        holder.activeIndicator.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.profileCard.context, color))
        holder.profileName.text = profile.name
        holder.profileCard.setOnClickListener {
            profileSelect(index, profile)
        }
        holder.profileEdit.setOnClickListener {
            profileEdit(profile)
        }
        holder.profileDelete.setOnClickListener {
            profileDelete(profile)
        }
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int): Boolean {
        profiles.add(toPosition, profiles.removeAt(fromPosition))
        notifyItemMoved(fromPosition, toPosition)
        if (toPosition > fromPosition) {
            notifyItemRangeChanged(fromPosition, toPosition - fromPosition + 1)
        } else {
            notifyItemRangeChanged(toPosition, fromPosition - toPosition + 1)
        }
        return true
    }

    override fun onItemMoveCompleted(startPosition: Int, endPosition: Int) {
        val isMoveUp = startPosition > endPosition
        val start = if (isMoveUp) profiles[endPosition+1] else profiles[endPosition-1]
        val end = profiles[endPosition]
        scope.launch {
            if (isMoveUp) profileViewModel.moveUp(start.index, end.index, end.id)
            else profileViewModel.moveDown(start.index, end.index, end.id)
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        var activeIndicator: LinearLayout = item.findViewById(R.id.activeIndicator)
        var profileCard: CardView = item.findViewById(R.id.profileCard)
        var profileName: TextView = item.findViewById(R.id.profileName)
        var profileEdit: LinearLayout = item.findViewById(R.id.profileEdit)
        var profileDelete: LinearLayout = item.findViewById(R.id.profileDelete)
    }
}
