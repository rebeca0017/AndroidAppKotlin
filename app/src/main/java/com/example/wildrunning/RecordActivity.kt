package com.example.wildrunning

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RecordActivity : AppCompatActivity() {
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        toolbar = findViewById(R.id.toolbar_record)
        setSupportActionBar(toolbar)

        toolbar.title = getString(R.string.bar_title_record)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.gray_dark))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.order_records_by, menu)
        toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.gray_dark))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.orderby_date -> {
                toggleItemTitle(item, R.string.orderby_dateZA, R.string.orderby_dateAZ)
                return true
            }
            R.id.orderby_duration -> {
                toggleItemTitle(item, R.string.orderby_durationZA, R.string.orderby_durationAZ)
                return true
            }
            R.id.orderby_distance -> {
                toggleItemTitle(item, R.string.orderby_distanceZA, R.string.orderby_distanceAZ)
                return true
            }
            R.id.orderby_avgspeed -> {
                toggleItemTitle(item, R.string.orderby_avgspeedZA, R.string.orderby_avgspeedAZ)
                return true
            }
            R.id.orderby_maxspeed -> {
                toggleItemTitle(item, R.string.orderby_maxspeedZA, R.string.orderby_maxspeedAZ)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleItemTitle(item: MenuItem, descText: Int, ascText: Int) {
        item.title = if (item.title == getString(descText)) {
            getString(ascText)
        } else {
            getString(descText)
        }
    }

    fun loadRunsBike(v: View) {
    }

    fun loadRunsRollerSkate(v: View) {
    }

    fun loadRunsRunning(v: View) {
    }

    fun callHome(v: View) {
        finish()
    }
}
