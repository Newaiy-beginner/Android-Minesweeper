package com.example.termprojectdemo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.termprojectdemo.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import java.util.*
import java.util.Random
import kotlin.math.min

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generateButton.setOnClickListener {
            val nRow: Int? = binding.nRowEditText.text.toString().toIntOrNull()
            val nCol: Int? = binding.nColumnEditText.text.toString().toIntOrNull()
            val nMines: Int? = binding.nMines.text.toString().toIntOrNull()

            // if any value above is invalid, we do nothing
            if (nRow != null && nCol != null && nMines != null && nMines in 0..nRow * nCol) {
                binding.generateButton.isEnabled = false

                val mList = generateMineList(nRow, nCol, nMines)
                val valueList = valueListgenerator(nRow, nCol, mList)
                MineMap(this, nRow, nCol, mList, valueList).apply { showMineMap() }
            }
        }

        binding.resetButton.setOnClickListener {
            binding.generateButton.isEnabled = true
            binding.mainView.removeAllViews()
        }
    }

    private fun generateMineList(nRow: Int, nCol: Int, nMine: Int): List<Pair<Int, Int>> {
        return generateRandomMineList(nRow, nCol, nMine)
    }

    private fun generateRandomMineList(nRow: Int, nCol: Int, nMine: Int): List<Pair<Int,Int>> {
        var minels: MutableList<Pair<Int, Int>>  = mutableListOf()
        var i = 1
        while(i <= nMine) {
            val randPair = Pair(Random().nextInt(nRow), Random().nextInt(nCol))
            if(!minels.contains(randPair)) {
                minels.add(randPair)
                i = i + 1
            }
        }
        return minels
    }

    private fun valueListgenerator(nRow: Int, nCol: Int, mineList: List<Pair<Int, Int>>): MutableList<MutableList<Int>> {
        val valueList: MutableList<MutableList<Int>> = mutableListOf()
        for (i in 1..nRow) {
            val sList = mutableListOf(0)
            for (j in 2..nCol)
                sList.add(0)
            valueList.add(sList)
        }
        val shift = listOf(-1, 0, 1)
        var x: Int
        var y: Int
        for (i in 1..mineList.size)
            valueList[mineList[i-1].first][mineList[i-1].second] = -1
        for (i in 1..mineList.size) {
            for (kr in 0..2) {
                x = mineList[i-1].first + shift[kr]
                if (x >= 0 && x < nRow) {
                    for (kc in 0..2) {
                        y = mineList[i-1].second + shift[kc]
                        if (y >= 0 && y < nCol) {
                            if (Pair(x, y) != mineList[i-1] && valueList[x][y] >= 0)
                                valueList[x][y] = valueList[x][y] + 1
                        }
                    }
                }
            }
        }
        return valueList
    }
}

class MineMap(
    private val context: Context,
    private val nRow: Int,
    private val nCol: Int,
    private val mineList: List<Pair<Int, Int>>,
    private val valueList: MutableList<MutableList<Int>>
) {
    private val dpScale = context.resources.displayMetrics.density.roundToInt()
    private val buttonMargin = 2 * dpScale

    private val buttonSize =
        ((minOf(
            context.resources.displayMetrics.xdpi / nCol,
            context.resources.displayMetrics.ydpi / nRow
        ) - buttonMargin * 2) * 0.95).roundToInt() * dpScale

    private val mineSweeperButtonList: List<List<MineButton>> =
        List(nRow) {
            List(nCol) {
                MineButton(context, buttonSize, buttonMargin)
            }
        }

    init {
        mineSweeperButtonList.forEachIndexed { rowIndex, rowItems ->
            rowItems.forEachIndexed { colIndex, item ->
                item.setOnClickListener { openMineMap(rowIndex, colIndex) }
            }
        }
    }

    private fun showAllMine() {
        for(i in 0..(mineList.size-1))
            mineSweeperButtonList[mineList[i].first][mineList[i].second].boom()
    }
    private var detectedMine = false
    private val edgeCoor = listOf(Pair(-1,1),Pair(-1,0),Pair(-1,-1),Pair(0,-1),Pair(1,-1),Pair(1,0),Pair(1,1),Pair(0,1))
    private fun openMineMap(rowIndex: Int, colIndex: Int) {
        if(!(detectedMine || valueList[rowIndex][colIndex] == -1)) {
            mineSweeperButtonList[rowIndex][colIndex].open(valueList[rowIndex][colIndex])
            if(valueList[rowIndex][colIndex] != 0) { return }
            valueList[rowIndex][colIndex] = -2 //走過的0標記起來
            //general case
            for(i in 0..7) {
                if (rowIndex + edgeCoor[i].first >= 0 && rowIndex + edgeCoor[i].first < nRow
                    && colIndex + edgeCoor[i].second >= 0 && colIndex + edgeCoor[i].second < nCol) {
                    mineSweeperButtonList[rowIndex + edgeCoor[i].first][colIndex + edgeCoor[i].second]
                        .open(valueList[rowIndex + edgeCoor[i].first][colIndex + edgeCoor[i].second])
                    if(valueList[rowIndex + edgeCoor[i].first][colIndex + edgeCoor[i].second] == 0)
                        openMineMap(rowIndex + edgeCoor[i].first, colIndex + edgeCoor[i].second)
                }
            }
            return
        }
        else if(!detectedMine) {
            mineSweeperButtonList[rowIndex][colIndex].boom()
            showAllMine()
            detectedMine = true
        }
    }

    fun showMineMap() {
        val layoutList: List<LinearLayout> =
            List(mineSweeperButtonList.size) { LinearLayout(context) }

        layoutList.forEach {
            it.orientation = LinearLayout.HORIZONTAL
            it.gravity = Gravity.CENTER
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        mineSweeperButtonList.forEachIndexed { rowIndex, rowItems ->
            rowItems.forEach {
                layoutList[rowIndex].addView(it)
            }
        }
        layoutList.forEach { binding.mainView.addView(it) }
    }
}

class MineButton(context: Context?, buttonSize: Int, buttonMargin: Int) :
    androidx.appcompat.widget.AppCompatButton(context!!) {
    // You probably need to define more properties and functions

    init {
        layoutParams =
            LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                topMargin = buttonMargin
                bottomMargin = buttonMargin
                leftMargin = buttonMargin
                rightMargin = buttonMargin
                textSize = 20F
            }
        setPadding(0, -5, 0, 0)
        setBackgroundColor(Color.parseColor("#558b2f"))
    }

    // you may need to modify this function
    fun open(v:Int) {
        setBackgroundColor(Color.parseColor("#9e9d24"))
        if(v > 0) { setText("$v") }
    }

    // you may need to modify this function
    fun boom() {
        setBackgroundColor(Color.parseColor("#e80c0f"))
        setText("*")
    }
}