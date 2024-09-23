package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.possystembw.database.User

@Dao
interface UserDao {
/*  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertUser(user: User): Long
 */
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertUser(user: User): Long

  @Update
  suspend fun updateUser(user: User)

  @Delete
  suspend fun delete(user: User)

  @Query("SELECT * FROM logins WHERE email = :email LIMIT 1")
  suspend fun getUserByEmail(email: String): User?

  @Query("SELECT * FROM logins")
  suspend fun getAllUsers(): List<User>

  @Query("SELECT * FROM logins WHERE email = :email AND password = :password LIMIT 1")
  suspend fun getUser(email: String, password: String): User?

  @Query("SELECT * FROM logins LIMIT 1")
  suspend fun getCurrentUser(): User?
  @Query("SELECT * FROM logins WHERE id = :userId")
  suspend fun getUserById(userId: String): User?
  @Transaction
  suspend fun upsertAll(users: List<User>) {
    users.forEach { user ->
      val id = insertUser(user)
      if (id == -1L) {
        updateUser(user)
      }
    }
  }
}