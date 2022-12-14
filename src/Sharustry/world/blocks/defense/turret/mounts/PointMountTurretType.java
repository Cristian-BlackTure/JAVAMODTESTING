package Sharustry.world.blocks.defense.turret.mounts;

import Sharustry.world.blocks.defense.turret.MultiTurret;
import arc.Core;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;

public class PointMountTurretType extends MountTurretType {
    public Color colorPoint = Color.white;
    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.pointHit;

    public float bulletDamage = 10f;
    public PointMountTurretType(String name) {
        super(name);
    }
    @Override
    public MountTurret create(MultiTurret block, MultiTurret.MultiTurretBuild build, int index, float x, float y) {
        return new PointMountTurret(this, block, build, index, x, y);
    }
    @Override
    public void buildStat(Table table) {
        super.buildStat(table);
        table.add("[lightgray]" + Core.bundle.format("stat.shar.targetsBullet") + ": [white]" + Core.bundle.get("yes")).row();
    }
    public class PointMountTurret extends MountTurret<PointMountTurretType> {
        public Bullet pointTarget;

        public PointMountTurret(PointMountTurretType type, MultiTurret block, MultiTurret.MultiTurretBuild build, int i, float x, float y) {
            super(type, block, build, i, x, y);
        }

        @Override
        public void updateTile() {
            super.updateTile();

            pointTarget = Groups.bullet.intersect(x - type.range, y - type.range, type.range * 2, type.range * 2).min(b -> b.team != build.team && b.type().hittable, b -> b.dst2(new Vec2(x, y)));

            if(pointTarget != null
                    && pointTarget.within(new Vec2(x, y), range)
                    && pointTarget.team != build.team
                    && pointTarget.type() != null
                    && pointTarget.type().hittable){
                float dest = build.angleTo(pointTarget);
                turnToTarget(dest);
                reloadCounter += build.delta() * getPowerEfficiency();

                //shoot when possible
                if(Angles.within(rotation, dest, shootCone) && reloadCounter >= reload){
                    if(pointTarget.damage() > bulletDamage) pointTarget.damage(pointTarget.damage() - bulletDamage);
                    else pointTarget.remove();

                    Tmp.v1.trns(rotation, shootLength);

                    beamEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, colorPoint, pointTarget);
                    shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, colorPoint);
                    hitEffect.at(pointTarget.x, pointTarget.y, colorPoint);
                    shootSound.at(x + Tmp.v1.x, y + Tmp.v1.y, Mathf.random(0.9f, 1.1f));

                    reloadCounter = 0f;
                }
            }
        }
    }
}
