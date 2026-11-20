package com.maan.integrationcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private var selectedFuncIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggleMode = findViewById<MaterialButtonToggleGroup>(R.id.toggleMode)
        val btnModePoly = findViewById<MaterialButton>(R.id.btnModePoly)
        val layoutPoly = findViewById<LinearLayout>(R.id.layoutPoly)
        val layoutFunc = findViewById<LinearLayout>(R.id.layoutFunc)

        val etPolyCoef = findViewById<TextInputEditText>(R.id.etPolyCoef)
        val actvFunction = findViewById<AutoCompleteTextView>(R.id.actvFunction)
        val etLowerA = findViewById<TextInputEditText>(R.id.etLowerA)
        val etUpperB = findViewById<TextInputEditText>(R.id.etUpperB)

        val chipSample1 = findViewById<Chip>(R.id.chipSample1)
        val chipSample2 = findViewById<Chip>(R.id.chipSample2)
        val chipSample3 = findViewById<Chip>(R.id.chipSample3)

        val btnIntegrate = findViewById<MaterialButton>(R.id.btnIntegrate)
        val btnClear = findViewById<MaterialButton>(R.id.btnClear)
        val btnCopy = findViewById<MaterialButton>(R.id.btnCopy)

        val tvFunctionPreview = findViewById<TextView>(R.id.tvFunctionPreview)
        val tvIndefiniteResult = findViewById<TextView>(R.id.tvIndefiniteResult)
        val tvDefiniteResult = findViewById<TextView>(R.id.tvDefiniteResult)

        val functions = listOf(
            "sin(x)",
            "cos(x)",
            "sec²(x)",
            "eˣ",
            "1/x",
            "1/√x"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, functions)
        actvFunction.setAdapter(adapter)
        actvFunction.setText(functions[0], false)
        actvFunction.setOnItemClickListener { _, _, position, _ ->
            selectedFuncIndex = position
        }

        toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.btnModePoly) {
                layoutPoly.visibility = View.VISIBLE
                layoutFunc.visibility = View.GONE
            } else {
                layoutPoly.visibility = View.GONE
                layoutFunc.visibility = View.VISIBLE
            }
        }

        chipSample1.setOnClickListener {
            etPolyCoef.setText("2, 3, 1")
            etLowerA.setText("0")
            etUpperB.setText("2")
        }
        chipSample2.setOnClickListener {
            etPolyCoef.setText("4, -2, 6")
            etLowerA.setText("1")
            etUpperB.setText("3")
        }
        chipSample3.setOnClickListener {
            etPolyCoef.setText("1, 0, 0, 4")
            etLowerA.setText("")
            etUpperB.setText("")
        }

        btnClear.setOnClickListener {
            etPolyCoef.setText("")
            etLowerA.setText("")
            etUpperB.setText("")
            tvFunctionPreview.text = "f(x) = —"
            tvIndefiniteResult.text = "∫ f(x) dx = Result yahan show hoga"
            tvDefiniteResult.text = "Definite [a, b]: Limits enter karein"
            tvDefiniteResult.setTextColor(0xFF94A3B8.toInt())
        }

        btnCopy.setOnClickListener {
            val combined = "${tvFunctionPreview.text}\n${tvIndefiniteResult.text}\n${tvDefiniteResult.text}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Integral Result", combined))
            Toast.makeText(this, "Result copied!", Toast.LENGTH_SHORT).show()
        }

        btnIntegrate.setOnClickListener {
            val aStr = etLowerA.text?.toString()?.trim().orEmpty()
            val bStr = etUpperB.text?.toString()?.trim().orEmpty()

            if (aStr.isNotEmpty() != bStr.isNotEmpty()) {
                tvDefiniteResult.text = "Definite integral ke liye 'a' aur 'b' dono enter karein."
                tvDefiniteResult.setTextColor(0xFFFCA5A5.toInt())
                return@setOnClickListener
            }

            val a = if (aStr.isNotEmpty()) aStr.toDoubleOrNull() else null
            val b = if (bStr.isNotEmpty()) bStr.toDoubleOrNull() else null

            if (aStr.isNotEmpty() && (a == null || b == null)) {
                tvDefiniteResult.text = "Ghalat limits! Valid numbers likhein."
                tvDefiniteResult.setTextColor(0xFFFCA5A5.toInt())
                return@setOnClickListener
            }

            val result = if (btnModePoly.isChecked) {
                integratePolynomial(etPolyCoef.text?.toString().orEmpty(), a, b)
            } else {
                integrateFunction(selectedFuncIndex, a, b)
            }

            tvFunctionPreview.text = result.fxLine
            tvIndefiniteResult.text = result.indefiniteLine
            tvDefiniteResult.text = result.definiteLine
            tvDefiniteResult.setTextColor(
                if (result.isError) 0xFFFCA5A5.toInt()
                else if (result.hasDefinite) 0xFF34D399.toInt()
                else 0xFF94A3B8.toInt()
            )
        }
    }

    private data class CalcResult(
        val fxLine: String,
        val indefiniteLine: String,
        val definiteLine: String,
        val hasDefinite: Boolean = false,
        val isError: Boolean = false
    )

    private fun toSuperscript(n: Int): String {
        val map = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '-' to '⁻'
        )
        return n.toString().map { map[it] ?: it }.joinToString("")
    }

    private fun formatTerm(coef: Double, power: Int, isFirst: Boolean): String {
        val sign = when {
            isFirst && coef < 0 -> "-"
            isFirst -> ""
            coef < 0 -> " - "
            else -> " + "
        }
        val absVal = abs(coef)
        val numStr = if (absVal % 1.0 == 0.0) absVal.toLong().toString() else "%.4g".format(absVal)
        val xPart = when (power) {
            0 -> ""
            1 -> "x"
            else -> "x${toSuperscript(power)}"
        }
        val coefDisplay = if (power > 0 && absVal == 1.0) "" else numStr
        return "$sign$coefDisplay$xPart"
    }

    private fun integratePolynomial(rawInput: String, a: Double?, b: Double?): CalcResult {
        val parts = rawInput.split(",", " ", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 51) {
            return CalcResult(
                fxLine = "f(x) = Invalid input",
                indefiniteLine = "1 se 51 tak coefficients likhein",
                definiteLine = "Example: 2, 3, 1",
                isError = true
            )
        }

        val coef = DoubleArray(parts.size)
        for (i in parts.indices) {
            coef[i] = parts[i].toDoubleOrNull() ?: return CalcResult(
                fxLine = "f(x) = Error",
                indefiniteLine = "Ghalat coefficient: ${parts[i]}",
                definiteLine = "Sirf numbers enter karein",
                isError = true
            )
        }

        val nonZeroFx = coef.indices.reversed().filter { coef[it] != 0.0 }
        val fxFormatted = if (nonZeroFx.isEmpty()) "0" else {
            nonZeroFx.mapIndexed { idx, power ->
                formatTerm(coef[power], power, idx == 0)
            }.joinToString("")
        }

        val intFormatted = if (nonZeroFx.isEmpty()) "C" else {
            nonZeroFx.mapIndexed { idx, power ->
                formatTerm(coef[power] / (power + 1), power + 1, idx == 0)
            }.joinToString("") + " + C"
        }

        if (a != null && b != null) {
            var sumA = 0.0
            var sumB = 0.0
            for (i in coef.indices) {
                if (coef[i] != 0.0) {
                    val c = coef[i] / (i + 1)
                    sumA += c * a.pow(i + 1)
                    sumB += c * b.pow(i + 1)
                }
            }
            return CalcResult(
                fxLine = "f(x) = $fxFormatted",
                indefiniteLine = "∫ f(x) dx = $intFormatted",
                definiteLine = "Definite [%g, %g] = %.6f".format(a, b, sumB - sumA),
                hasDefinite = true
            )
        }

        return CalcResult(
            fxLine = "f(x) = $fxFormatted",
            indefiniteLine = "∫ f(x) dx = $intFormatted",
            definiteLine = "Definite [a, b]: Optional limits enter karein"
        )
    }

    private fun integrateFunction(index: Int, a: Double?, b: Double?): CalcResult {
        data class FuncSpec(
            val name: String,
            val formula: String,
            val anti: (Double) -> Double?,
            val err: (Double) -> String?
        )

        val spec = when (index) {
            0 -> FuncSpec("sin(x)", "-cos(x) + C", { -cos(it) }, { null })
            1 -> FuncSpec("cos(x)", "sin(x) + C", { sin(it) }, { null })
            2 -> FuncSpec(
                "sec²(x)",
                "tan(x) + C",
                { if (abs(cos(it)) < 1e-12) null else tan(it) },
                { if (abs(cos(it)) < 1e-12) "tan(x) undefined at x = %g".format(it) else null }
            )
            3 -> FuncSpec("eˣ", "eˣ + C", { exp(it) }, { null })
            4 -> FuncSpec(
                "1/x",
                "ln|x| + C",
                { if (it == 0.0) null else ln(abs(it)) },
                { if (it == 0.0) "1/x undefined at x = 0" else null }
            )
            5 -> FuncSpec(
                "1/√x",
                "2√x + C",
                { if (it < 0.0) null else 2.0 * sqrt(it) },
                { if (it < 0.0) "x >= 0 hona chahiye" else null }
            )
            else -> return CalcResult("f(x) = —", "Invalid selection", "", isError = true)
        }

        if (a != null && b != null) {
            val errMsg = spec.err(a) ?: spec.err(b)
            if (errMsg != null) {
                return CalcResult(
                    fxLine = "f(x) = ${spec.name}",
                    indefiniteLine = "∫ ${spec.name} dx = ${spec.formula}",
                    definiteLine = errMsg,
                    isError = true
                )
            }
            val fa = spec.anti(a)!!
            val fb = spec.anti(b)!!
            return CalcResult(
                fxLine = "f(x) = ${spec.name}",
                indefiniteLine = "∫ ${spec.name} dx = ${spec.formula}",
                definiteLine = "Definite [%g, %g] = %.6f".format(a, b, fb - fa),
                hasDefinite = true
            )
        }

        return CalcResult(
            fxLine = "f(x) = ${spec.name}",
            indefiniteLine = "∫ ${spec.name} dx = ${spec.formula}",
            definiteLine = "Definite [a, b]: Optional limits enter karein"
        )
    }
}
