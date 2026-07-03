package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QrCodeView(
    payload: String,
    location: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(260.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Company Logo Placeholder and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                // Leaf/Circle corporate emblem
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFFE21C26), RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFB300), RoundedCornerShape(2.dp))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CPRAM (สุราษฎร์ธานี)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B365D),
                    fontFamily = FontFamily.SansSerif
                )
            }

            Text(
                text = "FIRE EXTINGUISHER ASSET",
                fontSize = 9.sp,
                fontWeight = FontWeight.Light,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic Custom QR Code Drawing Box
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                DeterministicQrCanvas(payload = payload)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Human Readable Tag metadata
            Text(
                text = payload,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.Black
            )

            Text(
                text = location,
                fontSize = 11.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Draws a fully realistic deterministic QR code matrix on Android Canvas using string hash values.
 * Models a standard 21x21 version 1 QR matrix with ISO-spec dual finder/anchor rings in 3 corners.
 */
@Composable
fun DeterministicQrCanvas(payload: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sizePx = size.width
        val moduleCount = 21 // Version 1 grid size
        val cellSize = sizePx / moduleCount

        // 1. Draw solid background
        drawRect(color = Color.White, size = size)

        // 2. Compute cells matrix deterministically based on payload hashcode
        val randomSource = java.util.Random(payload.hashCode().toLong())

        val grid = Array(moduleCount) { BooleanArray(moduleCount) }
        for (r in 0 until moduleCount) {
            for (c in 0 until moduleCount) {
                grid[r][c] = randomSource.nextBoolean()
            }
        }

        // 3. Clear Finder Patterns safezones (7x7 top-left, top-right, bottom-left)
        val finderSize = 7
        for (r in 0 until moduleCount) {
            for (c in 0 until moduleCount) {
                // Top-Left corner
                if (r < finderSize && c < finderSize) grid[r][c] = false
                // Top-Right corner
                if (r < finderSize && c >= moduleCount - finderSize) grid[r][c] = false
                // Bottom-Left corner
                if (r >= moduleCount - finderSize && c < finderSize) grid[r][c] = false
            }
        }

        // 4. Fill matrix random dots
        for (r in 0 until moduleCount) {
            for (c in 0 until moduleCount) {
                if (grid[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize + 0.5f, cellSize + 0.5f) // avoid microscopic spacing grid lines
                    )
                }
            }
        }

        // Helper function for finder anchors
        fun drawFinder(x: Float, y: Float) {
            // Outer 7x7 cell block
            drawRect(
                color = Color.Black,
                topLeft = Offset(x, y),
                size = Size(7 * cellSize, 7 * cellSize)
            )
            // Empty 5x5 cell block inside
            drawRect(
                color = Color.White,
                topLeft = Offset(x + cellSize, y + cellSize),
                size = Size(5 * cellSize, 5 * cellSize)
            )
            // Filled 3x3 block in the core center
            drawRect(
                color = Color.Black,
                topLeft = Offset(x + 2 * cellSize, y + 2 * cellSize),
                size = Size(3 * cellSize, 3 * cellSize)
            )
        }

        // Draw ISO-Standard Anchor Finder Squares!
        // Top Left
        drawFinder(0f, 0f)
        // Top Right
        drawFinder((moduleCount - finderSize) * cellSize, 0f)
        // Bottom Left
        drawFinder(0f, (moduleCount - finderSize) * cellSize)

        // Draw cute little safety center shield to make it branded!
        val cCenter = moduleCount / 2
        val shieldSize = 3 * cellSize
        val shieldX = (cCenter - 1) * cellSize
        val shieldY = (cCenter - 1) * cellSize

        drawRect(
            color = Color.White,
            topLeft = Offset(shieldX, shieldY),
            size = Size(shieldSize, shieldSize)
        )
        drawRect(
            color = Color(0xFFE21C26), // CPRAM Red center pixel crosshair for styling
            topLeft = Offset(shieldX + cellSize / 2f, shieldY + cellSize / 2f),
            size = Size(shieldSize - cellSize, shieldSize - cellSize)
        )
    }
}
