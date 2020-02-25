package deliangus.tech.aves

internal class KalmanFilter {
    var xk = arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))
    var pk = arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, 0f, 0f))
    var I = arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, 0f, 1f))
    var R = arrayOf(floatArrayOf(0.5f, 0f, 0f), floatArrayOf(0f, 0.5f, 0f), floatArrayOf(0f, 0f, 0.01f))
    var Q = arrayOf(floatArrayOf(0.005f, 0f, 0f), floatArrayOf(0f, 0.005f, 0f), floatArrayOf(0f, 0f, 0.001f))

    constructor()

    fun matrix_add(mata: Array<FloatArray>, matb: Array<FloatArray>): Array<FloatArray> {
        val matc = Array(matb.size) { FloatArray(matb[0].size) }
        for (i in 0..2) {
            for (j in 0..2) {
                matc[i][j] = mata[i][j] + matb[i][j]
            }
        }
        return matc
    }

    fun matrix_sub(mata: Array<FloatArray>, matb: Array<FloatArray>): Array<FloatArray> {
        val matc = Array(matb.size) { FloatArray(matb[0].size) }
        for (i in 0..2) {
            for (j in 0..2) {
                matc[i][j] = mata[i][j] - matb[i][j]
            }
        }
        return matc
    }

    fun matrix_multi(mata: Array<FloatArray>, matb: Array<FloatArray>): Array<FloatArray> {
        val matc = Array(mata.size) { FloatArray(matb[0].size) }
        for (i in matc.indices) {
            for (j in 0 until matc[0].size) {
                matc[i][j] = 0f
                for (k in 0 until mata[0].size) {
                    matc[i][j] += mata[i][k] * matb[k][j]
                }
            }
        }
        return matc
    }

    fun filter(am_angle_mat: Array<FloatArray>, gyro_angle_mat: Array<FloatArray>): FloatArray {
        val yk: Array<FloatArray>
        val pk_new: Array<FloatArray>
        val K: Array<FloatArray>
        val KxYk: Array<FloatArray>
        val I_K: Array<FloatArray>
        val S: Array<FloatArray>
        val S_invert = Array(3) { FloatArray(3) }
        xk = matrix_add(xk, gyro_angle_mat)
        pk = matrix_add(Q, pk)
        yk = matrix_sub(am_angle_mat, xk)
        S = matrix_add(R, pk)
        val sdet = S[0][0] * S[1][1] * S[2][2] + S[0][1] * S[1][2] * S[2][0] + S[0][2] * S[1][0] * S[2][1] - S[0][2] * S[1][1] * S[2][0] - S[1][2] * S[2][1] * S[0][0] - S[2][2] * S[0][1] * S[1][0]
        S_invert[0][0] = (S[0][1] * S[2][2] - S[1][2] * S[2][1]) / sdet
        S_invert[0][1] = (S[0][2] * S[2][1] - S[0][1] * S[2][2]) / sdet
        S_invert[0][2] = (S[0][1] * S[2][1] - S[1][1] * S[2][0]) / sdet
        S_invert[1][0] = (S[1][2] * S[2][0] - S[1][0] * S[2][2]) / sdet
        S_invert[1][1] = (S[0][0] * S[2][2] - S[0][2] * S[2][0]) / sdet
        S_invert[1][2] = (S[0][2] * S[1][0] - S[0][0] * S[1][2]) / sdet
        S_invert[2][0] = (S[1][0] * S[2][1] - S[1][1] * S[2][0]) / sdet
        S_invert[2][1] = (S[0][1] * S[2][0] - S[0][0] * S[2][1]) / sdet
        S_invert[2][2] = (S[0][0] * S[1][1] - S[0][1] * S[1][0]) / sdet
        K = matrix_multi(pk, S_invert)
        KxYk = matrix_multi(K, yk)
        xk = matrix_add(xk, KxYk)
        I_K = matrix_sub(I, K)
        pk_new = matrix_multi(I_K, pk)
        for (i in 0..2) {
            for (j in 0..2) {
                pk[i][j] = pk_new[i][j]
            }
        }
        return floatArrayOf(xk[0][0], xk[1][1], xk[2][2])
    }
}