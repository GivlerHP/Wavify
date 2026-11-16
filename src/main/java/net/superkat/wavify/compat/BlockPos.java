package net.superkat.wavify.compat;

/** Local positions, independent of world height and modern Minecraft classes. */
public class BlockPos {
    protected int x, y, z;
    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos(BlockPos p) {
        this(p.x, p.y, p.z);
    }

    public static BlockPos containing(double x, double y, double z) {
        return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BlockPos offset(int x, int y, int z) {
        return new BlockPos(this.x + x, this.y + y, this.z + z);
    }

    public BlockPos above() {
        return offset(0, 1, 0);
    }

    public MutableBlockPos mutable() {
        return new MutableBlockPos().set(x, y, z);
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockPos))
            return false;
        BlockPos b = (BlockPos) o;
        return x == b.x && y == b.y && z == b.z;
    }

    public static Iterable<BlockPos> betweenClosed(BlockPos a, BlockPos b) {
        java.util.List<BlockPos> r = new java.util.ArrayList<BlockPos>();
        for (int x = Math.min(a.x, b.x); x <= Math.max(a.x, b.x); x++)
            for (int y = Math.min(a.y, b.y); y <= Math.max(a.y, b.y); y++)
                for (int z = Math.min(a.z, b.z); z <= Math.max(a.z, b.z); z++)
                    r.add(new BlockPos(x, y, z));
        return r;
    }
    public static final class MutableBlockPos extends BlockPos {
        public MutableBlockPos() {
            super(0, 0, 0);
        }

        public MutableBlockPos set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public void move(int x, int y, int z) {
            this.x += x;
            this.y += y;
            this.z += z;
        }
    }
}
