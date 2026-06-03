package com.example.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodels.ThriftSixViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeamScreen(viewModel: ThriftSixViewModel) {
    val invites by viewModel.allInvites.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val userRole = viewModel.currentUserRole.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("team_screen_container")
    ) {
        // --- TITLE ---
        Text(
            text = "Collaboration & Action Auditing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Invite managers, assign secure roles, and inspect complete audit ledgers",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- ACTIVE ACCOUNT ROLE SWITCHER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Role",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Account: ${viewModel.currentUserEmail.value}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Role pill
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (viewModel.currentUserRole.value) {
                                    "Admin" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    "Editor" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else -> Color.DarkGray.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = viewModel.currentUserRole.value,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = when (viewModel.currentUserRole.value) {
                                "Admin" -> MaterialTheme.colorScheme.primary
                                "Editor" -> MaterialTheme.colorScheme.secondary
                                else -> Color.LightGray
                            }
                        )
                    }
                }

                val isFirebaseUser = com.example.data.FirebaseSyncHelper.isFirebaseAvailable() && 
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isFirebaseUser) {
                        "🔒 Privilege escalation locked. Active role is securely bound to your cloud-verified user profile."
                    } else {
                        "Evaluate system limits by instantly toggling role permission tiers:"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFirebaseUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val roles = listOf("Admin", "Editor", "Viewer")
                    roles.forEach { role ->
                        val isSelected = viewModel.currentUserRole.value == role
                        OutlinedButton(
                            onClick = {
                                if (isFirebaseUser) {
                                    viewModel.showToast("Privilege changes locked. Roles must be managed by the secure Firebase dashboard.")
                                } else {
                                    viewModel.currentUserRole.value = role
                                    viewModel.updateSessionActivity()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            enabled = !isFirebaseUser || isSelected,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("role_pill_${role.lowercase()}"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = role, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab selection (Invites vs Audit Logs ledger)
        var tabIndex by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
                Text("Team Invites (${invites.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
                Text("System Audit Logs (${auditLogs.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tabIndex == 0) {
            // --- TEAM INVITE FLOW ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (userRole == "Admin") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Invite Corporate Associate",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.inviteEmailInput.value,
                                onValueChange = { viewModel.inviteEmailInput.value = it },
                                placeholder = { Text("email@thriftsix.com") },
                                label = { Text("Invite Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invite_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Assign Store Authority Level:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val roles = listOf("Admin", "Editor", "Viewer")
                                roles.forEach { role ->
                                    val isSelected = viewModel.inviteRoleInput.value == role
                                    OutlinedButton(
                                        onClick = { viewModel.inviteRoleInput.value = role },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .testTag("invite_role_${role.lowercase()}"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = role, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.sendInvite() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_invite_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Store Credentials Invite", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Invite features are restricted. Elevated Admin level signature credentials are required.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }
                }

                Text(
                    text = "Awaiting Invitee Activations registry:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(invites) { inv ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("team_invite_card_${inv.email.replace("@", "_")}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inv.email, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Role authorization: ${inv.role}", fontSize = 11.sp, color = Color.Gray)
                                        if (userRole == "Admin") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(Change)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    val nextRole = when (inv.role) {
                                                        "Admin" -> "Editor"
                                                        "Editor" -> "Viewer"
                                                        else -> "Admin"
                                                    }
                                                    viewModel.updateTeamMemberRole(inv.email, nextRole)
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Status badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (inv.status) {
                                                    "Accepted" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    "Declined" -> Color.Red.copy(alpha = 0.15f)
                                                    else -> Color.Gray.copy(alpha = 0.12f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = inv.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (inv.status) {
                                                "Accepted" -> Color(0xFF10B981)
                                                "Declined" -> Color.Red
                                                else -> Color.DarkGray
                                            }
                                        )
                                    }

                                    if (userRole == "Admin") {
                                        IconButton(
                                            onClick = { viewModel.removeTeamMember(inv.email) },
                                            modifier = Modifier.size(28.dp).testTag("delete_member_btn_${inv.email.replace("@", "_")}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Team Member",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- SYSTEM AUDIT LOGS LEDGER ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Historical Store Modification Ledgers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs) { log ->
                        val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("audit_log_${log.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.action,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = date,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = log.details,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "By: ${log.userEmail}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
