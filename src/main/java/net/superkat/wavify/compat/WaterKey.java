package net.superkat.wavify.compat;
public final class WaterKey { private WaterKey(){} public static long chunk(int x,int z){return (x&0xffffffffL)|((long)z<<32);} }
