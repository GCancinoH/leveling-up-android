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
import com.gcancino.levelingup.domain.models.Question

@Composable
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
            text       = question.text,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            lineHeight = 28.sp
        )

        if (question.hint.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "💡 ${question.hint}",
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
}