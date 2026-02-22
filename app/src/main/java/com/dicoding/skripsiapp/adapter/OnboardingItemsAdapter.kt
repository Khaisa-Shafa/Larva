package com.dicoding.skripsiapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.data.OnBoardingItem

class OnboardingItemsAdapter(private val onboardingItems: List<OnBoardingItem>): RecyclerView.Adapter<OnboardingItemsAdapter.OnboardingItemViewHolder>() {

    inner class OnboardingItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

        private val imageOnboarding = view.findViewById<ImageView>(R.id.image_onboarding)
        private val textTitle = view.findViewById<TextView>(R.id.text_title)
        private val textDescription = view.findViewById<TextView>(R.id.text_description)

        fun bind(onBoardingItem: OnBoardingItem) {
            imageOnboarding.setImageResource(onBoardingItem.onboardingImage)
            textTitle.text = onBoardingItem.title
            textDescription.text = onBoardingItem.description

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingItemViewHolder {
        return OnboardingItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.onboarding_item_container,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingItemViewHolder, position: Int) = holder.bind(onboardingItems[position])

    override fun getItemCount(): Int = onboardingItems.size
}