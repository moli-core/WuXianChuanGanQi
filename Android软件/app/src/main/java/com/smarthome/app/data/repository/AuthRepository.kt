package com.smarthome.app.data.repository

import com.smarthome.app.core.datastore.TokenDataStore
import com.smarthome.app.core.model.LoginResponse
import com.smarthome.app.core.model.User
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.AuthApi
import com.smarthome.app.data.remote.dto.LoginRequest
import com.smarthome.app.data.remote.dto.RegisterRequest

class AuthRepository constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun login(username: String, password: String): NetworkResult<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    val data = body.data
                    val token = data?.get("token") as? String ?: return NetworkResult.Error(401, "登录失败：未获取到Token")
                    val userId = (data["userId"] as? Double)?.toInt() ?: 0
                    val userName = data["username"] as? String ?: ""
                    val nickName = data["nickname"] as? String ?: ""
                    val role = data["role"] as? String ?: "user"

                    tokenDataStore.saveAuthData(token, userId, userName, nickName, role)

                    NetworkResult.Success(LoginResponse(token, userId, userName, nickName, role))
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "登录失败")
                }
            } else {
                NetworkResult.Error(response.code(), "登录失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun register(
        username: String,
        password: String,
        nickname: String? = null,
        phone: String? = null,
        email: String? = null
    ): NetworkResult<String> {
        return try {
            val response = authApi.register(RegisterRequest(username, password, nickname, phone, email))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(body.data ?: "注册成功")
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "注册失败")
                }
            } else {
                NetworkResult.Error(response.code(), "注册失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getProfile(): NetworkResult<User> {
        return try {
            val response = authApi.getProfile()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取用户信息失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取用户信息失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun updateProfile(nickname: String?, phone: String?, email: String?): NetworkResult<User> {
        return try {
            val body = mutableMapOf<String, String>()
            nickname?.let { body["nickname"] = it }
            phone?.let { body["phone"] = it }
            email?.let { body["email"] = it }
            val response = authApi.updateProfile(body)
            if (response.isSuccessful) {
                val resp = response.body()
                if (resp != null && resp.isSuccess && resp.data != null) {
                    NetworkResult.Success(resp.data)
                } else {
                    NetworkResult.Error(resp?.code ?: 500, resp?.message ?: "修改失败")
                }
            } else {
                NetworkResult.Error(response.code(), "修改失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): NetworkResult<String> {
        return try {
            val body = mapOf("oldPassword" to oldPassword, "newPassword" to newPassword)
            val response = authApi.changePassword(body)
            if (response.isSuccessful) {
                val resp = response.body()
                if (resp != null && resp.isSuccess) {
                    NetworkResult.Success(resp.data ?: "密码修改成功")
                } else {
                    NetworkResult.Error(resp?.code ?: 500, resp?.message ?: "密码修改失败")
                }
            } else {
                NetworkResult.Error(response.code(), "密码修改失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun logout() {
        tokenDataStore.clearAuth()
    }
}
