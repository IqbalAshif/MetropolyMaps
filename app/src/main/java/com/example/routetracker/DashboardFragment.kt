package com.example.routetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment


class DashboardFragment(val mapfragment : MapFragment? = null) : Fragment() {

    companion object {
        fun newInstance(mapfragment : MapFragment) = DashboardFragment(mapfragment)
    }

    /* UI */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val card1 = view.findViewById<Button>(R.id.cardView1)
        card1.setOnClickListener { onClick(card1) }
        val card2 = view.findViewById<Button>(R.id.cardView2)
        card2.setOnClickListener { onClick(card2) }
        val card3 = view.findViewById<Button>(R.id.cardView3)
        card3.setOnClickListener { onClick(card3) }
        val card4 = view.findViewById<Button>(R.id.cardView4)
        card4.setOnClickListener { onClick(card4) }
        val card5 = view.findViewById<Button>(R.id.cardView5)
        card5.setOnClickListener { onClick(card5) }
        val card6 = view.findViewById<Button>(R.id.cardView6)
        card6.setOnClickListener { onClick(card6) }

        return view
    }

    private fun onClick(view : View): Unit? = mapfragment?.fetchPointsOfInterest(view.tag as String)
}
