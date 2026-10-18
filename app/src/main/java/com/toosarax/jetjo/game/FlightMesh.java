package com.toosarax.jetjo.game;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** Small reusable VBO meshes with flat or smooth surface normals. Built once per GL context. */
final class FlightMesh {
    private final FloatBuffer vertices;
    private final int count;
    private int buffer;
    private FlightMesh(float[] data) {
        vertices = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); vertices.put(data).position(0); count = data.length / 6;
    }
    void upload() {
        int[] id = new int[1]; GLES20.glGenBuffers(1, id, 0); buffer = id[0]; if (buffer == 0) throw new IllegalStateException("mesh buffer");
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer); GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.capacity() * 4, vertices, GLES20.GL_STATIC_DRAW); GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
    void draw(int position, int normal) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer); GLES20.glEnableVertexAttribArray(position); GLES20.glEnableVertexAttribArray(normal);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 24, 0); GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 24, 12); GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
    }
    void delete() { if (buffer != 0) { GLES20.glDeleteBuffers(1, new int[]{buffer}, 0); buffer = 0; } }
    private static final class Builder {
        final float[] data; int at;
        Builder(int triangles) { data = new float[triangles * 18]; }
        void vertex(float x, float y, float z, float nx, float ny, float nz) { data[at++] = x; data[at++] = y; data[at++] = z; data[at++] = nx; data[at++] = ny; data[at++] = nz; }
        void triangle(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
            float ux = bx - ax, uy = by - ay, uz = bz - az, vx = cx - ax, vy = cy - ay, vz = cz - az;
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx; float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length <= 0) length = 1; nx /= length; ny /= length; nz /= length;
            vertex(ax, ay, az, nx, ny, nz); vertex(bx, by, bz, nx, ny, nz); vertex(cx, cy, cz, nx, ny, nz);
        }
        void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
            triangle(ax, ay, az, bx, by, bz, cx, cy, cz); triangle(ax, ay, az, cx, cy, cz, dx, dy, dz);
        }
        FlightMesh build() { if (at != data.length) throw new IllegalStateException("mesh bounds"); return new FlightMesh(data); }
    }
    static FlightMesh cube() {
        Builder b = new Builder(12);
        b.quad(-.5f, .5f, .5f, .5f, .5f, .5f, .5f, .5f, -.5f, -.5f, .5f, -.5f);
        b.quad(-.5f, -.5f, -.5f, .5f, -.5f, -.5f, .5f, -.5f, .5f, -.5f, -.5f, .5f);
        b.quad(-.5f, -.5f, .5f, .5f, -.5f, .5f, .5f, .5f, .5f, -.5f, .5f, .5f);
        b.quad(.5f, -.5f, -.5f, -.5f, -.5f, -.5f, -.5f, .5f, -.5f, .5f, .5f, -.5f);
        b.quad(.5f, -.5f, .5f, .5f, -.5f, -.5f, .5f, .5f, -.5f, .5f, .5f, .5f);
        b.quad(-.5f, -.5f, -.5f, -.5f, -.5f, .5f, -.5f, .5f, .5f, -.5f, .5f, -.5f); return b.build();
    }
    static FlightMesh octahedron() {
        Builder b = new Builder(8);
        for (int i = 0; i < 4; i++) { float a = i * (float)Math.PI * .5f, n = (i + 1) * (float)Math.PI * .5f;
            float x = (float)Math.cos(a), z = (float)Math.sin(a), nx = (float)Math.cos(n), nz = (float)Math.sin(n);
            b.triangle(0, 1, 0, nx, 0, nz, x, 0, z); b.triangle(0, -1, 0, x, 0, z, nx, 0, nz);
        } return b.build();
    }
    static FlightMesh ship() {
        float[] x = {0, .44f, 1.15f, .35f, 0, -.35f, -1.15f, -.44f};
        float[] z = {-1.45f, -.25f, .95f, .60f, 1.08f, .60f, .95f, -.25f}; Builder b = new Builder(x.length * 4);
        for (int i = 0; i < x.length; i++) { int next = (i + 1) % x.length;
            b.triangle(0, .20f, 0, x[next], .20f, z[next], x[i], .20f, z[i]);
            b.triangle(0, -.20f, 0, x[i], -.20f, z[i], x[next], -.20f, z[next]);
            b.quad(x[i], -.20f, z[i], x[i], .20f, z[i], x[next], .20f, z[next], x[next], -.20f, z[next]);
        } return b.build();
    }
    static FlightMesh cone() {
        final int segments = 12; Builder b = new Builder(segments * 2);
        for (int i = 0; i < segments; i++) { float a = i * (float)Math.PI * 2 / segments, n = (i + 1) * (float)Math.PI * 2 / segments;
            float x = (float)Math.cos(a) * .5f, z = (float)Math.sin(a) * .5f, nx = (float)Math.cos(n) * .5f, nz = (float)Math.sin(n) * .5f;
            b.triangle(x, 0, z, nx, 0, nz, 0, 1, 0); b.triangle(0, 0, 0, nx, 0, nz, x, 0, z);
        } return b.build();
    }
    static FlightMesh sphere() {
        final int longitude = 16, latitude = 10; Builder b = new Builder(longitude * latitude * 2);
        for (int lat = 0; lat < latitude; lat++) for (int lon = 0; lon < longitude; lon++) {
            float a = (float)Math.PI * lat / latitude, n = (float)Math.PI * (lat + 1) / latitude;
            float l = (float)Math.PI * 2 * lon / longitude, m = (float)Math.PI * 2 * (lon + 1) / longitude;
            smooth(b, a, l); smooth(b, n, l); smooth(b, n, m); smooth(b, a, l); smooth(b, n, m); smooth(b, a, m);
        } return b.build();
    }
    private static void smooth(Builder b, float a, float l) {
        float x = (float)Math.sin(a) * (float)Math.cos(l), y = (float)Math.cos(a), z = (float)Math.sin(a) * (float)Math.sin(l); b.vertex(x, y, z, x, y, z);
    }
    static FlightMesh ring() {
        final int segments = 32; Builder b = new Builder(segments * 2);
        for (int i = 0; i < segments; i++) { float a = i * (float)Math.PI * 2 / segments, n = (i + 1) * (float)Math.PI * 2 / segments;
            float x = (float)Math.cos(a), z = (float)Math.sin(a), nx = (float)Math.cos(n), nz = (float)Math.sin(n);
            b.quad(x, 0, z, nx, 0, nz, nx * .84f, 0, nz * .84f, x * .84f, 0, z * .84f);
        } return b.build();
    }
    static FlightMesh disk() {
        final int segments = 24; Builder b = new Builder(segments);
        for (int i = 0; i < segments; i++) { float a = i * (float)Math.PI * 2 / segments, n = (i + 1) * (float)Math.PI * 2 / segments;
            b.triangle(0, 0, 0, (float)Math.cos(n), 0, (float)Math.sin(n), (float)Math.cos(a), 0, (float)Math.sin(a));
        } return b.build();
    }
}
