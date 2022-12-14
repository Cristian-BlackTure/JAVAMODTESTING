package Sharustry.world.blocks.defense.turret.mounts;

import Sharustry.content.SBullets;
import Sharustry.world.blocks.defense.turret.DriverBulletData;
import Sharustry.world.blocks.defense.turret.MultiTurret;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.MassDriver;

import static mindustry.Vars.*;
import static mindustry.Vars.content;

public class MassMountTurretType extends MountTurretType {
    public int minDistribute = 10;
    public int maxDistribute = 300;
    public float translation = 7f;
    public float knockback = 4f;
    public float bulletSpeed = 5.5f;
    public float bulletLifetime = 200f;
    public Effect receiveEffect = Fx.mineBig;
    public MassMountTurretType(String name) {
        super(name);
    }
    @Override
    public MountTurret create(MultiTurret block, MultiTurret.MultiTurretBuild build, int index, float x, float y) {
        return new MassMountTurret(this, block, build, index, x, y);
    }
    @Override
    public void drawPlace(MultiTurret block, int mount, int x, int y, int rotation, boolean valid) {
        super.drawPlace(block, mount, x, y, rotation, valid);
        float tX = x * tilesize + block.offset + block.mountOffsets.get(mount)[0];
        float tY = y * tilesize + block.offset + block.mountOffsets.get(mount)[1];

        //check if a mass driver is selected while placing this driver
        if(!control.input.config.isShown()) return;
        Building selected = control.input.config.getSelected();
        if(selected == null || !(selected.block instanceof MultiTurret) || !(selected.within(x * tilesize, y * tilesize, range))) return;

        //if so, draw a dotted line towards it while it is in range
        float sin = Mathf.absin(Time.time, 6f, 1f);
        Tmp.v1.set(tX, tY).sub(selected.x, selected.y).limit((block.size / 2f + 1) * tilesize + sin + 0.5f);
        float x2 = x * tilesize - Tmp.v1.x, y2 = y * tilesize - Tmp.v1.y,
                x1 = selected.x + Tmp.v1.x, y1 = selected.y + Tmp.v1.y;
        int segs = (int)(selected.dst(x * tilesize, y * tilesize) / tilesize);

        Lines.stroke(4f, Pal.gray);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Lines.stroke(2f, Pal.placing);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Draw.reset();
    }

    public class MassMountTurret extends MountTurret<MassMountTurretType> {
        public MassDriver.DriverState massState = MassDriver.DriverState.idle;
        public OrderedMap<Integer, Integer> waitingShooters = new OrderedMap<>();
        /** target multi turret's pos */
        public int link = -1;
        /** target multi turret's mount index */
        public int linkIndex = -1;

        public MassMountTurret(MassMountTurretType type, MultiTurret block, MultiTurret.MultiTurretBuild build, int i, float x, float y) {
            super(type, block, build, i, x, y);
        }

        @Nullable ObjectMap.Entry<Integer, Integer> currentShooter(){
            return !waitingShooters.iterator().hasNext() ? null : waitingShooters.iterator().next();
        }

        public boolean shooterValid(int pos, int index) {
            return world.build(pos) instanceof MultiTurret.MultiTurretBuild turret && turret.mounts.get(index) instanceof MassMountTurret mass
                    && mass.link == build.tile.pos()
                    && build.within(turret, mass.type.range);
        }
        public boolean linkValid(){
            return world.build(link) instanceof MultiTurret.MultiTurretBuild turret && linkIndex != -1 && turret.mounts.get(linkIndex) instanceof MassMountTurret
                    && build.within(turret, type.range);
        }

        @Override
        public void handlePayload(Bullet bullet, DriverBulletData data) {
            int totalItems = build.items.total();

            //add all the items possible
            for(int i = 0; i < data.items.length; i++){
                int maxAdd = Math.min(data.items[i], block.itemCapacity * 2 - totalItems);
                build.items.add(content.item(i), maxAdd);
                data.items[i] -= maxAdd;
                totalItems += maxAdd;

                if(totalItems >= block.itemCapacity * 2){
                    break;
                }
            }

            Effect.shake(type.shake, type.shake, build);
            type.receiveEffect.at(bullet);

            reloadCounter = 0f;
            bullet.remove();
        }

        @Override
        public void drawConfigure() {
            float sin = Mathf.absin(Time.time, 6 / 2f, 1f);

            Draw.color(Pal.accent);
            Lines.stroke(1f);
            Drawf.circles(x, y, (block.size  / 2f/ 2f + 1) * tilesize + sin - 2f, Pal.accent);

            for(ObjectMap.Entry<Integer, Integer> entry : waitingShooters){
                Tile shooter = world.tile(entry.key);
                Drawf.circles(shooter.drawx(), shooter.drawy(), (block.size / 2f / 2f + 1) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(shooter.drawx(), shooter.drawy(), x, y, block.size / 2f * tilesize + sin, 4f + sin, Pal.place);
            }

            if(linkValid()){
                MultiTurret.MultiTurretBuild target = (MultiTurret.MultiTurretBuild) world.build(link);
                MountTurret mount = target.mounts.get(linkIndex);
                Drawf.circles(mount.x, mount.y, (target.block().size / 2f / 2f + 1) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(x, y, mount.x, mount.y, block.size / 2f * tilesize + sin, 4f + sin);
            }

            Drawf.dashCircle(x, y, type.range, Pal.accent);
        }

        @Override
        public boolean acceptItem(Item item) {
            return build.items.total() < block.itemCapacity;
        }

        @Override
        public void updateTile() {
            super.updateTile();
            Building link = world.build(this.link);
            @Nullable ObjectMap.Entry<Integer, Integer> shooter = currentShooter();
            boolean hasLink = linkValid();
            if (hasLink) this.link = link.tile.pos();

            //?????????
            if (reloadCounter < reload) reloadCounter += build.delta() * getPowerEfficiency();
            //???????????? ?????? ??? ?????????
            if (shooter != null && !shooterValid(shooter.key, shooter.value)) {
                MassMountTurret mount = (MassMountTurret) ((MultiTurret.MultiTurretBuild) world.build(shooter.key)).mounts.get(shooter.value);

                if(mountIndex == 3) Log.info(mount.link + ": " + world.tile(mount.link));

                waitingShooters.remove(shooter.key);
            }

            //switch states
            if (massState == MassDriver.DriverState.idle) {
                //????????? ????????? ??????????????? ??????, ????????? ??????????????? accepting
                if (!waitingShooters.isEmpty() && (block.itemCapacity - build.items.total() >= minDistribute)) {
                    massState = MassDriver.DriverState.accepting;
                } else if (hasLink) { //?????? ????????? ???????????? ?????? ????????? ????????? shooting
                    massState = MassDriver.DriverState.shooting;
                }
            }

            //dump when idle or accepting
            if (massState == MassDriver.DriverState.idle || massState == MassDriver.DriverState.accepting)
                build.dump();

            //skip when there's no power
            if (getPowerEfficiency() <= 0.001f) return;

            if (massState == MassDriver.DriverState.accepting) {
                //if there's nothing shooting at this, bail - OR, items full
                if (shooter == null || (block.itemCapacity - build.items.total() < minDistribute)) {
                    massState = MassDriver.DriverState.idle;
                    return;
                }

                rotation = Mathf.slerpDelta(rotation, build.angleTo(world.tile(shooter.key)), rotateSpeed * getPowerEfficiency());
            } else if (massState == MassDriver.DriverState.shooting) {
                MassMountTurret linkedMount = (MassMountTurret) ((MultiTurret.MultiTurretBuild) link).mounts.get(linkIndex);
                //if there's nothing to shoot at OR someone wants to shoot at this thing, bail
                if (!hasLink || linkedMount == null || (!waitingShooters.isEmpty() && block.itemCapacity - build.items.total() >= minDistribute) || block.itemCapacity - link.items.total() < linkedMount.type.minDistribute) {
                    massState = MassDriver.DriverState.idle;
                    return;
                }
                //must shoot minimum amount of items and have minimum amount of space
                if (build.items.total() >= minDistribute && link.block.itemCapacity - link.items.total() >= minDistribute) {
                    linkedMount.waitingShooters.put(build.tile.pos(),mountIndex);

                    if (reloadCounter >= reload && !charging) {
                        float targetRotation = Angles.angle(x, y, linkedMount.x, linkedMount.y);
                        rotation = Mathf.slerpDelta(rotation, targetRotation, rotateSpeed * getPowerEfficiency());
                        //fire when it's the first in the queue and angles are ready.
                        if (linkedMount.currentShooter() != null && linkedMount.currentShooter().key == build.tile.pos()
                                && linkedMount.massState == MassDriver.DriverState.accepting
                                && Angles.near(rotation, targetRotation, 2f)
                                && Angles.near(linkedMount.rotation, targetRotation + 180f, 2f)) {
                            reloadCounter = 0f;

                            DriverBulletData data = Pools.obtain(DriverBulletData.class, DriverBulletData::new);
                            data.from = build;
                            data.to = link;
                            data.linkIndex = linkIndex;

                            int totalUsed = 0;
                            for (int h = 0; h < content.items().size; h++) {
                                int maxTransfer = Math.min(build.items.get(content.item(h)), block.itemCapacity - totalUsed);
                                data.items[h] = maxTransfer;
                                totalUsed += maxTransfer;
                                build.items.remove(content.item(h), maxTransfer);
                            }

                            float angle = Angles.angle(x, y, linkedMount.x, linkedMount.y);

                            SBullets.mountDriverBolt.create(build, build.team,
                                    x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation),
                                    angle, -1f, bulletSpeed, bulletLifetime, data);

                            shootEffect.at(x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation), angle);
                            smokeEffect.at(x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation), angle);
                            Effect.shake(shake, shake, new Vec2(x, y));
                            shootSound.at(build, Mathf.random(0.9f, 1.1f));

                            Time.run(Math.min(bulletLifetime, Mathf.dst(x, y, linkedMount.x, linkedMount.y) / bulletSpeed), () -> {
                                MassMountTurret currentLinkedMount = (MassMountTurret) ((MultiTurret.MultiTurretBuild) link).mounts.get(linkIndex);
                                //remove waiting shooters, it's done firing
                                currentLinkedMount.waitingShooters.remove(build.tile.pos());
                                currentLinkedMount.massState = MassDriver.DriverState.idle;
                            });
                            //driver is immediately idle
                            massState = MassDriver.DriverState.idle;
                        }
                    }
                }
            }
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(link);
            write.i(linkIndex);
            write.i(massState.ordinal());
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            link = read.i();
            linkIndex = read.i();
            massState = MassDriver.DriverState.all[read.i()];
        }
    }
}
