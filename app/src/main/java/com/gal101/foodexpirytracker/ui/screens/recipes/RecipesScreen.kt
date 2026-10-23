package com.gal101.foodexpirytracker.ui.screens.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// Dummy Data Models
data class ExpiringIngredient(
        val name: String,
        val imageUrl: String,
        val daysLeft: String,
        val color: Color
)

data class RecipeMatch(
        val title: String,
        val imageUrl: String,
        val prepTime: String,
        val rating: String,
        val reviews: String,
        val missingItems: Int,
        val statusColor: Color,
        val statusIcon: ImageVector
)

val dummyIngredients =
        listOf(
                ExpiringIngredient(
                        "Tomatoes",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuAxaZtgMrqU2YFFPsdmUAyOHJe0tlSm93qaPrQa-O-_nq5m5NPmrsXSW1GUSM866MfcnCayXsjLqTVzjG9x5O7NSY5uboaFkLenbIZ6GtZFpuC0R2npYMzHK_hc8cG4P_yS8EP8C0I19b2JWZuDNXLkDLQmGmmOTodT5nWIKYY7S3iXQLEaPBvfnFdCh7io59OJCH3oJizCRvt4nppprFvn1rFRTMZED3GgNUTWPGsbgIwIA4ozgl1u03nabdO792QNDxytjoN3dmLJ",
                        "2 Days",
                        Color(0xFFE53935)
                ),
                ExpiringIngredient(
                        "Milk",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDygtEkb2m8EWI4G5SF7eELr89ljHNSquMubTAu1ZtOqYcZyihopooX_2YPs8E90_2iy2Z1xvZ8mz-LSqFdI9DcIhqniDFWzsZoXqvIFH2TGlIfDmwsKP0dAYHNeXDyEH4pzsL0IRy-nD1NuvJ8LyEm_lkYjpFJs3nTQl543JFH1b0YRzuKLs3AfYrG9EbSfkwqxXVO-TA_NF6ljQ4rWLDDqnZKpWyZsH1PnzQ_qNBRcQcSRA-E3cYMvDj_7lsnfOml-8DB3O4D00kL",
                        "1 Day",
                        Color(0xFFFB8C00)
                ),
                ExpiringIngredient(
                        "Spinach",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuALTlCKduTgnphVlSmau9d_gwBXWBKd_MwqZewn1JCV5dG3DizT2TRv2yS8HJ5TEYnecEXMB7ss6Pb16imS1LVkJYyFB29SF2sIrL-GGMfz2WmZ5vyrvqvgJU7DeOjRFSNBZi2D-x3rVJ9y-MPwjqsVLaU5IdTwTwVnqw0FBZlM-zMAAcKNR6Qj4TQxbfUTrKLJanN6fr3I-ek4KTnaZe8gxXSCuBoMVjUJ7kLJEEeRbpun4YLTQKYMGiunIut40OX65QlaK8YZQgkW",
                        "Expiring",
                        Color(0xFFE53935)
                ),
                ExpiringIngredient(
                        "Cheddar",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDb0PgU-QHoaUqfcC27-YNvyjq1TsA-yC7t7A1Cn8n8WTcsiFLbAfLGo4PAPjsRgof_OzTXi934ZKjDqXIoh5n7BuJyfOxfqJ1uujt256BakfhtUFeJAleWWGWYUDdhsKosEl81VvrbjACKrgqJphxBz5XbRKZxe4mwwO9xGGBY2XVGHD5SJotWR9cwdfS4Ru18UuhKlkCDJIgtzBumcDihriCY5cRZZnBMsqckJ1jUoPkTtOZt6B6IiSE5DEj8n1xMO9ftTWHdQjFE",
                        "4 Days",
                        Color(0xFF1E88E5)
                ),
                ExpiringIngredient(
                        "Carrots",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuAYX1yreUp26PIwirIFljQ2oJNUGbDxBYZSzIcUogETxXuHNTmywWONSoQ_5i_KH9nZWdpg693yukrUCfc1KxdDniPUcSIx8DCPHHjBk0dlk_UJNVvwL_RXEXsihMgp4M4MkoWeoMhjWOOjz0RPNiMWwBPr7BvHo-aA4_WjOz6liIVAc6r57W9XUkE1ZkbbnUpW6Ao8DeXf1ZHl-8tZGicVpiBYbFjLgHvGuNaT_5k7LpzSnKDvUGjCKDmeMt0F60XWNKH_l3G4_1gZ",
                        "5 Days",
                        Color(0xFF1E88E5)
                )
        )

val dummyRecipes =
        listOf(
                RecipeMatch(
                        "Spinach & Tomato Pasta",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuB-IvdriGgOdr5QEogqCOApShQSh55tzULG_OyEhyqEQFX5coY0Vajj0nbC9MomanJfOlJdGk_PSzSN7UWVVOZdk0NC79Cps-mLjdUM3DGpfC8op_b2_ZbgK5aFhLkL6gj5no2NjPHNGvA_ti_HA04bWR02pE-Gwh0awvwn4ikep_w9ncfmgBnvOvbRNzVC0bO9d0ihKIjG6LFFIVjgOggGVvPNRW8dZD5QQITdSusJ8PvIi0zUJgMzHjSSact4lWDg3kcUY748-QHS",
                        "20 min",
                        "4.8",
                        "124",
                        0,
                        Color(0xFF4CAF50),
                        Icons.Default.CheckCircle
                ),
                RecipeMatch(
                        "Creamy Mushroom Soup",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuC7clDZW24iYUuVv9yTbLNcgfdY44H2kcMrusT4Y5T5pc4mzCFi5vZ1A2XLGTB0Kwkaw5NQNRGwjqF-hGTi6TGxM8BlyKN1ijaBRZIl0vjPnrlfGBC4aLWZoj2Yd3HfrHRYsjXt0hX7H3UPtqCaVkpTTs5RVFHo9eAdyyKW8JSEZUH0ZCQwhpt_oVKDCclo8QFRR3SNC5ZLsinyfEzZC317oV3OXnkvTZot3Z3BdBjWM7MkTitgNpgH9sWO6tte205fr9rX57kb8sk4",
                        "35 min",
                        "4.5",
                        "89",
                        1,
                        Color(0xFFFF9800),
                        Icons.Default.Info
                ),
                RecipeMatch(
                        "Avocado Toast Supreme",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDIAJzurObSAFIeIw2kLqPNx4DqgFFjAhIY1xw9hZYS14PnHTExty6up27wgcWcutJoUYTi4GqD967MHC81hk4OK1eumkjfAt4Hb4r7taZIcJI1jtpt0ZfIJMchBaATMmTDixZ97_Hew1OBBtCN2VvZH3mcDV3O3ce42AqEl-hCLSi_Kp-fSuniJJaMwa316IxgDbcDEBwvEpRY3O3ewYuhpDWaspQEuDYzydmSnAx50xnLlIELZjEFXv1WmLLwYBbyRfpn0jDPIT1d",
                        "10 min",
                        "4.9",
                        "210",
                        0,
                        Color(0xFF4CAF50),
                        Icons.Default.CheckCircle
                ),
                RecipeMatch(
                        "Spicy Chicken Curry",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuA9z1uuKyo3H7xRArY-tC02V-593kP9zPaGe5DwGrG7cmSiEeQIU3wRtY-sHClP2nZ4EkMzCdkWb7M3UixBVUWn9l-cE-KI_fhPSPC9vP2yK_fYYSj5_0t7b1-R6zqovlzk1hCerowr90fV4jV-7dhrNTHluBYIAwGgsqTDr2cHKz-WMXdOdpHTjG5KLEsdQazw_ADBvfN-7PlH9Nc9eh_agLc1rItiTqiFsPScP3u0RSv0JI2-nKoV6BBgrXgI7Smb6I5sheG8HpUr",
                        "45 min",
                        "4.7",
                        "95",
                        2,
                        Color(0xFFF44336),
                        Icons.Default.Warning
                )
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen() {
    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text("Recipe Ideas", fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { /* TODO */}) {
                                Icon(Icons.Default.Search, contentDescription = "Search recipes")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Expiring Soon Section
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Expiring Soon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { /* TODO */}) { Text("View All") }
            }

            LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(dummyIngredients) { ingredient -> ExpiringIngredientItem(ingredient) } }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Matches For You Section
            Text(
                    text = "Matches for You",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
            ) { items(dummyRecipes) { recipe -> RecipeCard(recipe) } }
        }
    }
}

@Composable
fun ExpiringIngredientItem(ingredient: ExpiringIngredient) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Box(modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp))) {
            AsyncImage(
                    model = ingredient.imageUrl,
                    contentDescription = ingredient.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
            )
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color.Transparent,
                                                                    Color.Black.copy(alpha = 0.6f)
                                                            ),
                                                    startY = 48f
                                            )
                                    )
            )
            Surface(
                    color = ingredient.color.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            ) {
                Text(
                        text = ingredient.daysLeft,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = ingredient.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RecipeCard(recipe: RecipeMatch) {
    Card(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                )
                Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Time",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = recipe.prepTime,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bookmark FAB
                FilledIconButton(
                        onClick = { /* TODO */},
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
                                        .offset(x = (-8).dp, y = (16).dp)
                                        .size(40.dp),
                        colors =
                                IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                )
                ) {
                    Icon(
                            Icons.Default.BookmarkAdd,
                            contentDescription = "Save Recipe",
                            tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = recipe.rating,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = "(${recipe.reviews})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                        color = recipe.statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                recipe.statusIcon,
                                contentDescription = null,
                                tint = recipe.statusColor,
                                modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text =
                                        if (recipe.missingItems == 0) "You have all ingredients"
                                        else
                                                "Missing ${recipe.missingItems} item" +
                                                        if (recipe.missingItems > 1) "s" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = recipe.statusColor,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
