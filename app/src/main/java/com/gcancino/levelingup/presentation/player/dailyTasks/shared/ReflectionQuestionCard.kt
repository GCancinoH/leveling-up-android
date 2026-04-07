package com.gcancino.levelingup.presentation.player.dailyTasks.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.gcancino.levelingup.domain.models.Question

@Composable
fun ReflectionQuestionCard(
    question: Question,
    answer: String,
    onAnswerChange: (String) -> Unit,
    accentColor: Color = Color(0xFFFFB300)
) {
    // ── String resolution happens HERE in the composable, not in the ViewModel ────
    // This keeps the ViewModel Context-free while the UI gets readable strings.
    val questionText = stringResource(question.textRes)
    val hintText     = question.hint?.let { stringResource(it) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // Anchor badge
        if (question.isAnchor) {
            Surface(
                shape    = RoundedCornerShape(4.dp),
                color    = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text     = "Daily Anchor",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Question text
        Text(
            text       = questionText,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            lineHeight = 28.sp
        )

        // Hint
        if (hintText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "💡 $hintText",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Answer field
        OutlinedTextField(
            value         = answer,
            onValueChange = onAnswerChange,
            modifier      = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            placeholder   = {
                Text("Write your answer here...", color = Color.DarkGray)
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction      = ImeAction.Default
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = accentColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = accentColor
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Character counter / validation hint
        Text(
            text  = "${answer.trim().length} chars " +
                    if (answer.trim().length < 10) "· min 10 to continue" else "✓",
            style = MaterialTheme.typography.labelSmall,
            color = if (answer.trim().length >= 10) accentColor else Color.Gray
        )
    }
}

/**
 * Resolves a Question's textRes to a plain String.
 * Used by ViewModels that need the resolved text before saving
 * (e.g. to build the questionTexts map passed to save()).
 *
 * Usage in composable:
 *   val questionTexts = questions.resolveTexts()
 *   viewModel.save(questionTexts)
 */
@Composable
fun List<Question>.resolveTexts(): Map<String, String> =
    associate { question ->
        question.id to stringResource(question.textRes)
    }
/*@Composable
fun ReflectionQuestionCard(
    question: Question,
    answer: String,
    onAnswerChange: (String) -> Unit,
    accentColor: Color = Color(0xFFFFB300)
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (question.isAnchor) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    "Daily Anchor",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text       = stringResource(question.textRes),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            lineHeight = 28.sp
        )

        question.hint?.let { hintRes ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "💡 ${stringResource(hintRes)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value         = answer,
            onValueChange = onAnswerChange,
            modifier      = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            placeholder   = { Text("Write your answer here...", color = Color.DarkGray) },
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = accentColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = accentColor
            ),
            shape  = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "${answer.trim().length} chars ${if (answer.trim().length < 10) "· min 10 to continue" else "✓"}",
            style = MaterialTheme.typography.labelSmall,
            color = if (answer.trim().length >= 10) accentColor else Color.Gray
        )
    }
}*/