package com.example.possystembw.ui

import android.content.Context
import android.util.Log
import com.example.possystembw.data.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.util.Date

class LocalDataServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {
    private val database = AppDatabase.getDatabase(context)
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .setPrettyPrinting()
        .create()

    private val TAG = "LocalDataServer"

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        Log.d(TAG, "Request: $method $uri")

        return when {
            uri == "/transactions" && method == Method.GET -> {
                handleGetTransactions()
            }
            uri == "/transaction-summaries" && method == Method.GET -> {
                handleGetTransactionSummaries()
            }
            uri == "/transaction-details" && method == Method.GET -> {
                val transactionId = session.parms["transactionId"]
                handleGetTransactionDetails(transactionId)
            }
            uri == "/stats" && method == Method.GET -> {
                handleGetStats()
            }
            uri == "/" || uri == "/index.html" -> {
                handleGetIndex()
            }
            else -> {
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        }
    }

    private fun handleGetTransactions(): Response {
        return try {
            val responseJson = runBlocking {
                val transactions = database.transactionDao().getAllTransactionRecords()
                Log.d(TAG, "Found ${transactions.size} transaction records")

                gson.toJson(mapOf(
                    "success" to true,
                    "count" to transactions.size,
                    "data" to transactions
                ))
            }

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                responseJson
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transactions", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(mapOf("success" to false, "error" to e.message))
            )
        }
    }

    private fun handleGetTransactionSummaries(): Response {
        return try {
            val responseJson = runBlocking {
                val summaries = database.transactionDao().getAllTransactionSummaries()
                Log.d(TAG, "Found ${summaries.size} transaction summaries")

                gson.toJson(mapOf(
                    "success" to true,
                    "count" to summaries.size,
                    "data" to summaries
                ))
            }

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                responseJson
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transaction summaries", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(mapOf("success" to false, "error" to e.message))
            )
        }
    }

    private fun handleGetTransactionDetails(transactionId: String?): Response {
        return try {
            if (transactionId.isNullOrEmpty()) {
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    gson.toJson(mapOf("success" to false, "message" to "Transaction ID required"))
                )
            }

            val responseJson = runBlocking {
                val summary = database.transactionDao().getTransactionSummary(transactionId)
                val records = database.transactionDao().getTransactionRecordsByTransactionId(transactionId)

                Log.d(TAG, "Transaction $transactionId: Summary found: ${summary != null}, Records found: ${records.size}")

                gson.toJson(mapOf(
                    "success" to true,
                    "transactionId" to transactionId,
                    "summary" to summary,
                    "records" to records,
                    "recordCount" to records.size
                ))
            }

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                responseJson
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transaction details for ID: $transactionId", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(mapOf("success" to false, "error" to e.message))
            )
        }
    }

    private fun handleGetStats(): Response {
        return try {
            val responseJson = runBlocking {
                val totalTransactions = database.transactionDao().getAllTransactionSummaries().size
                val totalRecords = database.transactionDao().getAllTransactionRecords().size
                val syncedTransactions = database.transactionDao().getSyncedTransactionSummaries().size
                val unsyncedTransactions = database.transactionDao().getUnsyncedTransactionSummaries().size

                Log.d(TAG, "Stats - Total transactions: $totalTransactions, Total records: $totalRecords")

                gson.toJson(mapOf(
                    "success" to true,
                    "stats" to mapOf(
                        "totalTransactions" to totalTransactions,
                        "totalRecords" to totalRecords,
                        "syncedTransactions" to syncedTransactions,
                        "unsyncedTransactions" to unsyncedTransactions,
                        "syncPercentage" to if (totalTransactions > 0) {
                            (syncedTransactions.toDouble() / totalTransactions * 100).toInt()
                        } else 0
                    ),
                    "timestamp" to System.currentTimeMillis()
                ))
            }

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                responseJson
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stats", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(mapOf("success" to false, "error" to e.message))
            )
        }
    }

    private fun handleGetIndex(): Response {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>POS Data Viewer</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .endpoint { margin: 15px 0; padding: 15px; background: #f8f9fa; border-radius: 5px; border-left: 4px solid #007bff; }
                    .stats { background: #e8f4f8; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    button { padding: 12px 20px; margin: 5px; cursor: pointer; background: #007bff; color: white; border: none; border-radius: 4px; font-size: 14px; }
                    button:hover { background: #0056b3; }
                    #output { border: 1px solid #ddd; padding: 15px; margin: 15px 0; min-height: 300px; white-space: pre-wrap; background: #f8f9fa; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 12px; }
                    input[type="text"] { padding: 10px; margin: 5px; border: 1px solid #ddd; border-radius: 4px; width: 200px; }
                    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
                    .stat-card { background: white; padding: 15px; border-radius: 6px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .stat-number { font-size: 24px; font-weight: bold; color: #007bff; }
                    .stat-label { color: #6c757d; margin-top: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🏪 POS Local Data Viewer</h1>
                    <p>Real-time access to your local transaction database</p>
                    
                    <div class="stats">
                        <h3>📊 Database Statistics</h3>
                        <button onclick="loadStats()">🔄 Refresh Stats</button>
                        <div id="stats-output" class="stats-grid"></div>
                    </div>
                    
                    <div class="endpoint">
                        <h3>📋 Data Endpoints</h3>
                        <button onclick="loadData('/transactions')">📄 All Transaction Records</button>
                        <button onclick="loadData('/transaction-summaries')">📊 Transaction Summaries</button>
                        <button onclick="loadStats()">📈 Database Stats</button>
                    </div>
                    
                    <div class="endpoint">
                        <h3>🔍 Transaction Details</h3>
                        <input type="text" id="transactionId" placeholder="Enter Transaction ID">
                        <button onclick="loadTransactionDetails()">🔎 Load Details</button>
                    </div>
                    
                    <h3>📝 Output</h3>
                    <div id="output">Click any button above to load data...</div>
                </div>
                
                <script>
                    function loadData(endpoint) {
                        document.getElementById('output').textContent = 'Loading...';
                        fetch(endpoint)
                            .then(response => response.json())
                            .then(data => {
                                document.getElementById('output').textContent = JSON.stringify(data, null, 2);
                            })
                            .catch(error => {
                                document.getElementById('output').textContent = 'Error: ' + error;
                            });
                    }
                    
                    function loadStats() {
                        fetch('/stats')
                            .then(response => response.json())
                            .then(data => {
                                const statsDiv = document.getElementById('stats-output');
                                if (data.success) {
                                    const stats = data.stats;
                                    statsDiv.innerHTML = 
                                        '<div class="stat-card"><div class="stat-number">' + stats.totalTransactions + '</div><div class="stat-label">Total Transactions</div></div>' +
                                        '<div class="stat-card"><div class="stat-number">' + stats.totalRecords + '</div><div class="stat-label">Total Records</div></div>' +
                                        '<div class="stat-card"><div class="stat-number">' + stats.syncedTransactions + '</div><div class="stat-label">Synced</div></div>' +
                                        '<div class="stat-card"><div class="stat-number">' + stats.unsyncedTransactions + '</div><div class="stat-label">Unsynced</div></div>' +
                                        '<div class="stat-card"><div class="stat-number">' + stats.syncPercentage + '%</div><div class="stat-label">Sync Rate</div></div>';
                                } else {
                                    statsDiv.innerHTML = '<div class="stat-card"><div class="stat-label">Error loading stats</div></div>';
                                }
                            })
                            .catch(error => {
                                document.getElementById('stats-output').innerHTML = '<div class="stat-card"><div class="stat-label">Network error: ' + error + '</div></div>';
                            });
                    }
                    
                    function loadTransactionDetails() {
                        const transactionId = document.getElementById('transactionId').value.trim();
                        if (!transactionId) {
                            alert('Please enter a transaction ID');
                            return;
                        }
                        loadData('/transaction-details?transactionId=' + encodeURIComponent(transactionId));
                    }
                    
                    // Load stats on page load
                    document.addEventListener('DOMContentLoaded', function() {
                        loadStats();
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }
}