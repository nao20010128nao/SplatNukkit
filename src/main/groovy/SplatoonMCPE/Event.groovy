package SplatoonMCPE

import cn.nukkit.Player
import cn.nukkit.block.Block
import cn.nukkit.entity.Entity
import cn.nukkit.event.EventHandler
import cn.nukkit.event.Listener
import cn.nukkit.event.block.BlockBreakEvent
import cn.nukkit.event.block.BlockPlaceEvent
import cn.nukkit.event.entity.EntityDamageByEntityEvent
import cn.nukkit.event.entity.EntityDamageEvent
import cn.nukkit.event.entity.EntityDeathEvent
import cn.nukkit.event.entity.EntityRegainHealthEvent
import cn.nukkit.event.server.QueryRegenerateEvent
import cn.nukkit.level.particle.TerrainParticle
import cn.nukkit.level.sound.SplashSound
import cn.nukkit.math.Vector3

import static java.lang.Math.*

/**
 * Created by nao on 2017/05/03.
 */
// TODO: Use Plugin.getServer as much as possible, refrain from using Server.getInstance
class Event implements Listener{
    Main main
    Event(Main main){
        this.main=main
    }

    @EventHandler void QueryRegenerateEvent(QueryRegenerateEvent ev){
        ev.maxPlayerCount=40
    }
    @EventHandler void onBlockBreak(BlockBreakEvent event){
        def player=event.player
        if(!player.creative){
            event.cancelled=true
            return
        }
        def block=event.block
        if(block.id==65&&block.damage==0){
            def item=event.item
            if(item.id!=280){
                event.cancelled=true
                // TODO: localize
                player.sendTip("§e≫ 透明のはしごを破壊する場合は棒を持って行ってください")
            }
        }
    }
    @EventHandler void onBlockPlace(BlockPlaceEvent event) {
        def player=event.player
        def user=player.name
        def block=event.block
        switch (block.id){
            case 46:
                event.cancelled=true
                break
            default:
                if(!(player.op||main.canPaint(player))){
                    event.cancelled=true
                }
                break
        }
    }
    @EventHandler void onEntityDeath(EntityDeathEvent event){
        def entity=event.entity
        if(main.dev==2){
            if(Enemy.isEnemy(entity)){
                def color=5
                def level=main.server.defaultLevel
                def (x,y,z)=[entity.x,entity.y,entity.z]
                def paint=7//paint radius for killing
                for(def xx=-floor(paint/2);xx<ceil(paint/2);xx++){
                    for(def yy=-floor(paint/2);yy<ceil(paint/2);yy++){
                        for(def zz=-floor(paint/2);zz<ceil(paint/2);zz++){
                            if(level.getBlockIdAt((int)floor(xx+x), (int)floor(yy+y), (int)floor(zz+z))==35){
                                level.setBlockDataAt((int)floor(xx+x), (int)floor(yy+y), (int)floor(zz+z),color)
                            }
                        }
                    }
                }

                def f={array->
                    array[0].addParticle(array[1])
                }
                def block= Block.get(35,color)
                def radius=3.3
                def (radius1,radius3)=[radius/2,radius]
                def p=new Vector3(x,y,z) // Entity can also work as Vector3, but why?
                level.addSound(new SplashSound(p))
                final step=360/(PI*radius)
                for(def yaw=0;yaw<360;yaw+=step){
                    for(def pitch=0;pitch<360;pitch+=step){
                        def radY=yaw/180*PI
                        def radP=(pitch-180)/180*PI
                        def xx = sin(radY)*cos(radP)
                        def yy = sin(radP)
                        def zz = -cos(radY)*cos(radP)
                        p.x=x+xx*radius1
                        p.y=y+yy*radius1
                        p.z=z+zz*radius1
                        def particle1=new TerrainParticle(p,block)
                        level.addParticle(particle1)
                        p.x=x+xx*radius3
                        p.y=y+yy*radius3
                        p.z=z+zz*radius3
                        def particle3=new TerrainParticle(p,block)
                        level.addParticle(particle3)
                        main.server.scheduler.scheduleDelayedTask(new LateDo(this,f,[level,particle3]),3)
                    }
                }
                if(main.killCount){
                    main.killCount++
                    Enemy.killEnemyEvent(entity, main.killCount, main)
                }
                entity.close()
            }
        }
    }
    @EventHandler void onEntityDamage(EntityDamageEvent event){
        if(event instanceof EntityDamageByEntityEvent){
            if(event.damager.player && event.damager.ink){
                def canAttack=main.canAttack(event.damager.player.name,event.entity.name).result
                if(canAttack) {
                    event.entity.attack(new EntityDamageByEntityEvent(event.damager.player, event.entity, EntityDamageEvent.CAUSE_ENTITY_EXPLOSION, 6, 0))
                }
                event.cancelled=true
            }
        }
        if(Enemy.isEnemy(event.entity)){
            if(event.cause==EntityDamageEvent.CAUSE_FALL){
                event.cancelled=true
                return
            }
            def ent=event.entity
            ent.heal(new EntityRegainHealthEvent(ent,0,0))
            if((event instanceof EntityDamageByEntityEvent) && (event.damager instanceof Player)){
                ent.lastAttack=event.damager.name
            }
            return
        }
        if(!(event.entity instanceof Player)){
            event.cancelled=true
            return
        }
        def s = event.entity.name// attacked
        if(main.team.getBattleTeamOf(s) && main.checkFieldTeleport() && main.game!=10){
            event.cancelled=true
            return
        }
        switch(event.cause){
            case EntityDamageEvent.CAUSE_ENTITY_ATTACK:
            case EntityDamageEvent.CAUSE_ENTITY_EXPLOSION:
            case EntityDamageEvent.CAUSE_SUFFOCATION:
                if(main.Task.Respawn[s]){
                    event.cancelled=true
                    return
                }
                if(event instanceof EntityDamageByEntityEvent){
                    def __dmg=event.damager
                    if(__dmg instanceof Player){
                        def damager=event.damager.name// damager name
                        def result=main.canAttack(damager,s)
                        if(!result.result)event.cancelled=true
                        switch (result.reason){
                            case 1:return
                            case 2:
                                __dmg.sendPopup(main.lang.translateString('attackNotEnemy'))
                                return
                        }
                        def team
                        if(main.canAttack(event.damager.name,event.entity.name)&&(team=main.team.getBattleTeamOf(s))){
                            def fieldData=main.getBattleField(main.field)
                            def range
                            //TODO: Name-independent identification
                            switch(fieldData.name){
                                case 'イカ研究所-実験室A-':
                                    range = 7;
                                    break;

                                case 'ヤドカリ遺跡':
                                    range = 24;
                                    break;

                                case 'キダカ秘密基地':
                                    range = 6;
                                    break;

                                default:
                                    range = 12;
                                    break;
                            }
                            def f=fieldData.start[team]
                            def px=event.entity.x
                            def pz=event.entity.z
                            if(abs(px-f[0])<range && abs(pz-f[2])<range && main.canPaint(event.damager)){
                                main.onDeath(event.entity,event.damager,null,EntityDamageEvent.CAUSE_FIRE)
                                event.cancelled=true
                                return
                            }
                        }
                        def check={ Entity ent1, Entity ent2->
                            def level=main.server.defaultLevel
                            def (x1,y1,z1)=[ent1.x-0.5,ent1.y+1.5,ent1.z-0.5]
                            def (x2,y2,z2)=[ent2.x-0.5,ent2.y+1.5,ent2.z-0.5]

                            def my=max(y1,y2)

                            def maxDist=max(abs(x2 - x1), max(my, abs(z2 - z1)))

                            if(maxDist==0||sqrt(pow(x2-x1,2)+pow(z2-z1,2))<=1){
                                return true
                            }

                            def (xDist,zDist)=[(x2-x1)/maxDist,(z2-z1)/maxDist]

                            maxDist.times {times->
                                def bid=level.getBlockIdAt((int)floor(x1 + xDist * times), (int)floor(my), (int)floor(z1 + zDist * times))
                                if(main.w.canThrough(bid)){
                                    return false
                                }
                            }

                            return true
                        }

                        if(check(event.damager,event.entity)){
                            def weaponNum=Account.instance.getData(damager).nowWeapon
                            switch(weaponNum){
                                case Weapon.SPLATTERSHOT:
                                case Weapon.SPLATTERSHOT_JR:
                                case Weapon.SPLOOSH_O_MATIC:
                                case Weapon.SPLATTERSHOT_PRO:
                                case Weapon.GAL_96:
                                case Weapon.GAL_52:
                                case Weapon.DUAL_SQUELCHER:
                                case Weapon.SPLASH_O_MATIC:
                                    // weapons to enable knockback
                                    break;

                                default:
                                    //disable knockback
                                    event.knockBack=0
                            }
                        }else{
                            event.cancelled=true
                        }
                        // Event.php:358
                    }
                }
        }
    }
}
