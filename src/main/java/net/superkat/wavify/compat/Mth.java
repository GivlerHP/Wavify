package net.superkat.wavify.compat;

public final class Mth {
    private Mth() {
    }

    public static int floor(double x) {
        return (int) Math.floor(x);
    }

    public static int ceil(double x) {
        return (int) Math.ceil(x);
    }

    public static int clamp(int x, int a, int b) {
        return Math.max(a, Math.min(b, x));
    }

    public static float clamp(float x, float a, float b) {
        return Math.max(a, Math.min(b, x));
    }

    public static double clamp(double x, double a, double b) {
        return Math.max(a, Math.min(b, x));
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static float rotLerp(float t, float a, float b) {
        float d = (b - a) % 360;
        if (d >= 180)
            d -= 360;
        if (d < -180)
            d += 360;
        return a + t * d;
    }

    public static float sin(float x) {
        return (float) Math.sin(x);
    }

    public static double square(double x) {
        return x * x;
    }
}
