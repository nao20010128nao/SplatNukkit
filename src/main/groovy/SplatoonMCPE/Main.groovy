package SplatoonMCPE

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.entity.Entity
import cn.nukkit.lang.BaseLang
import cn.nukkit.plugin.Plugin
import cn.nukkit.plugin.PluginBase
import cn.nukkit.potion.Effect
import cn.nukkit.scheduler.Task

import java.util.stream.Collectors

import static java.lang.Math.*
import static SplatoonMCPE.PhpUtils.*
import static java.util.Collections.*

/**
 * Created by nao on 2017/05/03.
 */
class Main extends PluginBase {
    // TODO: Decide variable type of following
    // TODO: Better variable name

    //public  attribute = [];
    public mute = false
    public BattleResultAnimation = null
    public cam = []
    public chatData = []
    public count_time = 0
    public start_time = 0
    public dev = false
    public error = 0
    public field = 0
    public game = 1
    public gamestop = false
    private leftCheck = []
    public lobbyPos = [532.5, 8, -107.5]
    public quitCheck = []
    public reconData = [:]
    private scattersItem = []
    private squids = []
    //private Squid_Standby = [];
    public Task = [:]
    private textParticle
    private Timelimit
    public Tips = true
    //private tnt_data = [];
    public TPanimation = null
    public tprCheckData = []
    private unfinished = false
    public view = [:]
    private waterLevel = false
    public warn = []
    private winteam = []
    private tag = false
    private tagTeams = []

    private hasSpawn = false
    private scanBattleField_data = ""

    private woolsBlockArray = []
    private splatWoolsArray = []

    public mute_personal = []
    public op_only = false
    public needLv = 0

    private list_kaomoji = [
            "_(┐「ε:)_",
            "(*´q｀*)",
            "⊂('ω'⊂ )))Σ≡",
            "(*´ω｀*)",
            "(´･ω･｀)",
            "_(⌒(_´･ω･` )_",
            "(´*ω*｀)",
            "\\(* ･ω･ *)/",
            "(´,,･ω･,,｀)",
    ]

    public area = [
            'mode'   : false,
            'extra'  : [
                    'state'  : false,
                    'winteam': 0,
                    'time'   : 0
            ],
            'count'  : [
                    1: [
                            'c': 100, //countdown
                            'p': 0 //penalty time
                    ],
                    2: [
                            'c': 100, //countdown
                            'p': 0 //penalty time
                    ],
            ],
            'history': [//previous status of invasion
                        'team' : 0,
                        'start': 0,
                        'end'  : 0
            ],
            'area'   : [//invasion state per an area
                        1: 0,
                        2: 0
            ],
            'areaall': 0,//state of invasion
            'wool'   : [],//
            'wools'  : []
    ]

    public tweakPosition = [
            0: [-0.5, -0.5],
            1: [-0.5, 0.5],
            2: [0.5, -0.5],
            3: [0.5, 0.5]
    ]
    public trypaintData = [
            'player': [:],
            'status': [
                    501: true, 502: true, 503: true, 504: true, 505: true,
                    506: true, 507: true, 508: true, 509: true, 510: true,
                    511: true, 512: true, 513: true, 514: true,
            ]
    ]

    @Override
    void onLoad() {
        server.commandMap.registerAll('splaturn',[
                new StatusUnitCommand(this),
                new BanCommand(this),
                new WarnCommand(this),
                new SetModeCommand(this)
        ])
    }

    @Override
    void onEnable() {
        if(server.pluginManager.getPlugin('UniLoginSystem')){
            uniLoginSystem=server.pluginManager.getPlugin('UniLoginSystem')
            logger.info('§6ULSが見つかりました')
        }else{
            logger.warning('ULSが見つかりません')
        }
        server.expEnabled=true
        server.pluginManager.registerEvents(new Event(this),this)
        this.db=new DataBase()
        this.a=new Accont(this)
        this.s=new StatusUnit(this)
        this.lang=new BaseLang(new File(dataFolder,'lang').absolutePath,s.language)
        this.w=new Weapon(this)
        this.team=new Team(this)
        this.entry=new Entry(this)

        this.team.init()
        data=true
        this.itemSelect=new ItemSelect(this,w.weaponsDataAll,this.lang)
        this.itemCase=new ItemCase(this)
        this.seat=new Seat()
        Task.tips=server.scheduler.scheduleRepeatingTask(new RandomTask(this),20*75)
        s.loginRestriction
        s.resetOnlineStat()
        creativeItemDelete()
        w.weaponsDataAllIntoDB
        updateStage()
        this.shop=new ItemShop(this,w.weaponsDataAll)

        def num=40
        server.config.set('max-players',num)

        def property=Server.getField('maxPlayers')
        property.accessible=true
        property.set(server,num)

        switch(server.port){
            case 19133:
                area.mode=true
                needLv=10
                break
            default:
                area.mode=false
                needLv=0
                break
        }
    }

    @Override
    void onDisable() {
        // Main.php:267
        trypaintData.player.forEach{String user,data->
            def player
            if((player=server.getPlayer(user))instanceof Player){
                tryPaint(player,false,false)
            }
            this.a.saveAll(true)

            /*
            if($this->s->sno !== 1){//GameServer1
			    if($this->s->getServerStatus(1)){//GameServer1
			    	$s_ap = $this->s->getServerAP(1);//GameServer1
			    	$address = $s_ap[0];
				    $port = $s_ap[1];
				    foreach($this->getServer()->getOnlinePlayers() as $player){
				    	if($player instanceof Player){
				    		$packet = new TransferPacket();
				    		$packet->address = $address;
				    		$packet->port = $port;
					    	$player->dataPacket($packet);
				    	}
				    }
			    }
		    }
            */

            tpAnimationEnd()
            this.s.offline
            this.s.removeAllFromOnlineStat()
        }
    }

    /**
     * Set something about battle field
     * @param first true if the server is launching, false otherwise
     */
    void setData(boolean first=false){
        this.battleField=[
                //How to write battle field information
                // TODO: fill all the description
                // TODO: make all configurable from a single config
                // Make sure that key is required
                0:[
                        'name': '', // name of the field
                        'author': "", // creator of the field
                        'level': "", // name of Level where the field exists
                        'start': [1 : [0, 0, 0, 0, 0], 2 : [0, 0, 0, 0, 0]],
                        'scan': [1 : [0, 0, 0], 2 : [0, 0, 0]],
                        'respawn-view': [1 : [0, 0, 0, 0, 0], 2 : [0, 0, 0, 0, 0]],
                        'comment': '', // comment from author(s)
                        'area': [1: [[ 0, 0, 0],[ 0, 0, 0]],2: [[ 0, 0, 0],[ 0, 0, 0]]],
                        'color': 0,// wool damage value for resetting
                        'sec': 5,// span for scanning field
                        'time-limit': 120,// set 150 for bigger field
                        'spawn-radius': 2,// radius for respawn area (size of particle)
                        'view': [0, 0, 0],// destination to teleport for watching
                        // camera
                        // 4 is the center of the field, 5 is point A, 6 is point B (6 is not required)
                        'cam': [4 : [0, 0, 0, 0, 0], 5 : [0, 0, 0, 0, 0], 6 : [0, 0, 0, 0, 0]],
                        'recon': false// watching
                ],
                // Test painting area
                // keys must follow at trypaintData

                // for odd number key, there must be 'Lava farm'
                // for even number key, there must be 'Subway map'
                // in "scan" key, 1 is measured from 'Lava farm' and 'Groundwater dome', 2 is the opposite.
                'try':[
                        501: [
                                'level': "",// name of Level where the area exists
                                'start': [0,0,0,0,0],
                                'scan': [1: [0,0,0], 2: [0,0,0]],
                                'color': 0
                        ]
                ]
        ]

        def weaponsName=[:]
        def maxNum=w.weaponAmount
        for(def i=1;i<=maxNum;i++){
            weaponsName[i]='' // set weapons name here
        }
        this.w.weaponsName=weaponsName

        def subWeapName=[
                1:'',//name of sub weapon
                2:''
                //...
        ]
        this.w.subWeaponsName=subWeapName

        // this should be 'randomTips'
        this.randomChat=[
                '',// make sure that this is blank
        ]
        16.times{
            this.randomChat<<this.lang.translateString("randomTip.${it+1}")
        }

        def commandMap=server.commandMap
        description.commands.forEach{cmdName,data->
            def baseTxt="command.${cmdName}.description"
            def description=this.lang.translateString(baseTxt)
            // check translations and command exists
            def command
            if(baseTxt!=description && (command=commandMap.getCommand(cmdName)) instanceof Command){
                command.description=description
                command.usage=this.lang.translateString("command.${cmdName}.usage")
            }
        }

        if(!first){
            floatText(false)
            this.shop.resetAll()
            this.itemSelect.lang=this.lang
            this.itemSelect.weaponsData=this.w.weaponsDataAll
            server.onlinePlayers.each {
                this.itemSelect.floatingTextColorChange(player)
                this.itemSelect.addFloatingTextParticle(player)
            }
        }
    }

    // TODO: localize hardcoded messages
    @Override
    boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        def out=''
        def user=s.name
        switch(label){
            // For tests
            case 'test':
                switch (Integer.valueOf(a[0])){
                    case 1:
                        Command.broadcastCommandMessage(s,'水位下げ')
                        changeFieldForKusoStart()
                        break
                    case 2:
                        Command.broadcastCommandMessage(s,'水位上げ')
                        changeFieldForKusoLast()
                        break
                    case 3:
                        Command.broadcastCommandMessage(s,'スキャン完了')
                        this.kuso
                        break
                }
                break
            case 'test2':
                //TODO: who are they?
                def members=[
                        "moya4",
                        "43ki",
                        "53ki",
                        "63ki",
                        "73ki",
                        "83ki",
                        "trasta334"
                ]
                members.each {
                    this.entry.addEntry(it)
                }
                floatText=[0]
                Command.broadcastCommandMessage(s,this.lang.translateString("command.test.32kicorps.add"))
                break
            case 'dev':
                if(a.size()==0)return false
                switch (a[0]){
                    case "saveskin":
                        if(a.size()==2){
                            def skinName=a[1]
                            def savePlayer=server.getPlayer(skinName)
                            def result=Enemy.saveSkinData(savePlayer)
                            // TODO: why not s.sendMessage()?
                            if(result){
                                println "スキンをセーブしました"
                            }else{
                                println "そのプレイヤーは存在しません"
                            }
                        }
                        break
                    case "point":
                        if(a.size()==2){
                            def point=Integer.valueOf(a[1])
                            if(s instanceof Player){
                                def playerData=Account.instance.getData(user)
                                playerData.grantPoint(point)
                                s.sendMessage("$point ポイント追加")
                            }
                        }else if(a.size()==3){
                            def point=Integer.valueOf(a[1])
                            def pl=server.getPlayer(a[2])
                            if(pl instanceof Player){
                                def playerData=Account.instance.getData(user)
                                playerData.grantPoint(point)
                                [s,pl]*.sendMessage("$point ポイント追加")
                            }else if(s instanceof Player){
                                s.sendMessage('§4そのプレイヤーは存在しません')
                            }else{
                                // TODO: are you really an idiot?
                                println '§4そのプレイヤーは存在しません'
                            }
                        }
                        break
                }
                break
            case 'wp':
                def point=Integer.valueOf(a[1])
                if(s instanceof Player){
                    def playerData=Account.instance.getData(user)
                    def lv=playerData.giveExp(point)
                    s.sendMessage("$point 武器ポイント追加")
                }
                break
            case 'mute':
                if(this.mute){
                    server.broadcastMessage("サーバー内のチャット機能を有効にしました")
                }else{
                    server.broadcastMessage("サーバー内のチャット機能を無効にしました")
                }
                this.mute=!this.mute
                break
            case 'start':
                this.w.stopMoveTask()
                stopRepeating()
                if(Task.PositionCheck)server.scheduler.cancelTask(Task.PositionCheck.taskId)
                if(Task.game){
                    Task.game.each {task->
                        // Workaround for working TimeScheduler (/gend)
                        server.scheduler.cancelTask(task.taskId)
                    }
                }
                Command.broadcastCommandMessage(s,this.lang.translateString("command.dev.start"))
                this.dev=true
                this.game=1
                timeTable()
                break
            case 'pve':
                this.w.stopMoveTask()
                this.stopRepeating()
                if(Task.PositionCheck)server.scheduler.cancelTask(Task.PositionCheck.taskId)
                if(Task.game){
                    Task.game.each {task->
                        // Workaround for working TimeScheduler (/gend)
                        server.scheduler.cancelTask(task.taskId)
                    }
                }
                this.dev=2
                this.game=1
                timeTable()
                break
            case 'area':
                def lv=a[1]?:0
                needLv=lv
                if(area.mode){
                    server.broadcastMessage("ガチマッチに設定しました")
                }else{
                    server.broadcastMessage("レギュラーマッチに設定しました")
                }
                area.mode=!area.mode
                server.broadcastMessage("参加必須レベルを${lv}に設定しました")
                break
            case 'lv':
                def lv=a[1]?:0
                needLv=lv
                server.broadcastMessage("参加必須レベルを${lv}に設定しました")
                break
            case 'map':
                def m=a.size()
                if(m>0){
                    def st=""
                    def stArr=[:]
                    (1..m).each {i->
                        stArr[Integer.valueOf(a[i])]=floor(100/m)+1
                    }
                    def data=[
                            h:[date('H')]*2,
                            s:stArr
                    ]
                    if(this.s.setStageData(data,date('H'))){
                        floatText=[6]
                        Command.broadcastCommandMessage(s,"ステージを${st}に変更しました")
                    }else if(s instanceof Player){
                        s.sendMessage('失敗')
                    }
                }
                break
            case 'mapall':
                def m=a.size()
                if(m>0){
                    def data=[]
                    def st=""
                    def stArr=[:]
                    (1..m).each {i->
                        stArr[Integer.valueOf(a[i])]=floor(100/m)+1
                    }
                    24.times{t->
                        data<<[
                                h:[t]*2,
                                s:stArr
                        ]
                    }
                    if(this.s.setStageData(data,date('H'))){
                        floatText=[6]
                        Command.broadcastCommandMessage(s,"全てのステージを${st}に変更しました")
                    }else if(s instanceof Player){
                        s.sendMessage('失敗')
                    }
                }
                break
            case 'us':
                updateStage()
                floatText=[6]
                Command.broadcastCommandMessage(s,"ステージ情報を更新しました。")
                break
            case 'rank':
                if(a.size()==2){
                    user=a[1]
                    def player=server.getPlayer(user)
                    if(player instanceof Player){
                        user=player.name
                        def playerData=Account.instance.getData(user)
                        if(s instanceof Player){
                            s.sendMessage("$user ウデマエ $playerData.rank")
                        }
                    }
                }
                break
            case 'end':
                if(!dev)return false
                Command.broadcastCommandMessage(s,this.lang.translateString("command.dev.end"))
                def cnt=0
                switch(game){
                    case 3:
                        cnt=4
                        break
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                        cnt=3
                        break
                    case 10:
                        cnt=2
                        break
                    case 11:
                        cnt=1
                        break
                }
                cnt.times {
                    timeTable()
                }
                dev=false
                return true
            case 'field-reset':
                // do not delete double negation: cast to boolean is needed
                if(!!dev & !!field){
                    resetBattleField(field)
                    Command.broadcastCommandMessage(s,this.lang.translateString("command.dev.fieldReset"))
                }else{
                    out=this.lang.translateString("command.dev.fieldReset.failure")
                }
                break
            case 'only':
                // TODO: fix Japanese may be needed
                if(op_only){
                    out = "誰でも入室可能にしました"
                }else{
                    out = "opのみ入室を許可しました"
                }
                op_only=!op_only
                break
            // checks your place
            case 'xyz':
                if(s instanceof Player){
                    out=String.format("X: %6d, Y: %5d, Z:%6d\nYaw: %3d, Pitch: %3d",floor(s.x),floor(s.y),floor(s.z),s.yaw,s.pitch)
                }
                break
            // moves your angle for recording and so on
            case 'cam':
                if(view.containsKey(user)){
                    gameWatching(s,false,false)
                }
                if(reconData.containsKey(user)){
                    recon(s,false,false)
                }
                return camC(s,a)
            /////////////
            // management
            case 'setf':
                if(a.size()==0)return false
                def f=a[0]
                if(getBattleField(f) && f!=18){
                    nextField=f
                    Command.broadcastCommandMessage(s,this.lang.translateString("command.setf.success",getBattleField(f).name))
                    return true
                }else{
                    out=this.lang.translateString("field.notFound")
                }
                break
            // team
            case 't':
                if(a.size()>=1){
                    switch(a[0]){
                        case 'add':
                            if(this.s.hasOp(user)&& !s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            if(this.team.addTeam()){
                                Command.broadcastCommandMessage(s,this.lang.translateString("command.t.add.success"))
                                return true
                            }else{
                                out=this.lang.translateString("command.t.add.failure")
                            }
                            break
                        case 'remove':
                            if(!s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            def force=a.size()>2 & ['on','true','t','1'].contains(a[1])
                            def result=this.team.removeTeam(force)
                            if(result){
                                Command.broadcastCommandMessage(s,this.lang.translateString("command.t.remove.success"))
                                return true
                            }else{
                                out=this.lang.translateString("command.t.remove.failure")
                            }
                            break
                        case 'allquit':// kick everyone from team
                            if(!s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            def force=a.size()>2 & ['on','true','t','1'].contains(a[1])
                            def result=this.team.removeAllMember(force)
                            if(result){
                                Command.broadcastCommandMessage(s,this.lang.translateString("command.t.allQuit.success"))
                                return true
                            }else{
                                out=this.lang.translateString("command.t.allQuit.failure")
                            }
                            break
                        case 'shuffle':// shuffle members
                            if(!s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            def force=a.size()>2 & ['on','true','t','1'].contains(a[1])
                            def result=this.team.allMemberShuffle(force)
                            if(result){
                                Command.broadcastCommandMessage(s,this.lang.translateString("command.t.shuffle.success"))
                                return true
                            }else{
                                out=this.lang.translateString("command.t.shuffle.failure")
                            }
                            break
                        case 'event':// add, check about teams
                            if(!s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            def teamEvent=this.team.teamEvent
                            def teamName=[
                                    3 : "§eyellow",
                                    4 : "§agreen",
                                    5 : "§baqua",
                                    6 : "§9blue",
                                    7 : "§dpink",
                                    8 : "§5purple"
                            ]
                            out = "Team event\n§a> add§f"
                            if(teamEvent.add){
                                def addData=(Map)teamEvent.add
                                addData.keySet()
                                        .sort()
                                        .stream()
                                        .map{[it,addData[it]]}
                                        .each {// TODO: convert date format
                                    out+="\n   ${strPad(it[0],9)}§f : ${date("m/d H:i:s",it[1])}"
                                }
                            }
                            out += "\n§c> remove§f"
                            if(teamEvent.remove){
                                def removeData=(Map)teamEvent.remove
                                removeData.keySet()
                                        .sort()
                                        .stream()
                                        .map{[it,removeData[it]]}
                                        .each {// TODO: convert date format
                                    out+="\n   ${strPad(it[0],9)}§f : ${date("m/d H:i:s",it[1])}"
                                }
                            }
                            break
                        // reset game count
                        case "count-reset":
                        case "cr":
                        case "ct":
                            if(!s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            this.team.battleCountReset()
                            Command.broadcastCommandMessage(s,this.lang.translateString("command.t.gameCountReset"))
                            return true
                        // check game count
                        case "count-check":
                        case "c":
                        case "count":
                        case "cv":
                            if(!s.op){
                                s.sendMessage(this.lang.translateString("command.notPermission"))
                                return true
                            }
                            out=this.lang.translateString("command.gameCountList")
                            ((Map)this.team.teamBattleTime).forEach{teamNum,count->
                                out+="\n${strPad(this.team.getTeamName(teamNum))}§f : $count"
                            }
                            break
                        default:
                            return false
                    }
                }
                break
            // trasnfers players into another server
            // TODO: Do we REALLY need it?
            case 'tpalls':
                def sno
                if(a.size()>0&&!a[0].empty){
                    sno=a[0]
                }else{
                    sno=1
                }
                def serverName=this.s.getServerName(sno)
                if(serverName){
                    Command.broadcastCommandMessage(s,this.lang.translateString("command.t.tpalls.sender",serverName))
                    server.onlinePlayers.each {
                        this.s.gotoPlay(player,sno)
                    }
                }else{
                    out=this.lang.translateString("command.tpalls.serverNotFound")
                }
                break
            // Controls random messages
            case 'random':
                if(a.size()>0){
                    switch(a[0]){
                        case 'start':
                            if(Tips){
                                out=this.lang.translateString("command.random.error.alreadyStart")
                            }else{
                                Tips=true
                                Task.Tips=server.scheduler.scheduleRepeatingTask(new RandomTask(this), 20*75)
                                Command.broadcastCommandMessage(s,this.lang.translateString("command.random.start"))
                                return true
                            }
                            break
                        case 'stop':
                            if(!Tips){
                                out=this.lang.translateString("command.random.error.alreadyStop")
                            }else{
                                Tips=false
                                def task=Task.Tips.taskId
                                server.scheduler.cancelTask(task)
                                Command.broadcastCommandMessage(s,this.lang.translateString("command.random.stop"))
                                return true
                            }
                            break
                        default:
                            def num=Integer.valueOf(a[0])
                            if(num<=(this.randomChat.size()-1)){
                                out='GO'
                                randomBroad(num)
                                return true
                            }else{
                                out=this.lang.translateString("command.random.messageNotFound")
                            }
                    }
                }else{
                    randomBroad()
                }
                break
            // Private chat for operators
            case 'oc':
                if(a.size()==0)return false
                def message="[${(s instanceof Player)?s.displayName:s.name} -> OP] ${a.join(' ')}"
                server.logger.info(message)
                server.onlinePlayers.values().stream().filter{it.op}*.sendMessage(message)
                break
            // delete entities
            case 'del':
                Command.broadcastCommandMessage(s,this.lang.translateString("command.del.start"))
                def count=0
                def level=server.defaultLevel
                ((List<Entity>)level.entities.toList().stream().filter{!(it instanceof Player)}.collect(Collectors.toList())).with{
                    it*.close()
                    count=size()
                }
                Command.broadcastCommandMessage(s,this.lang.translateString("command.del.end",count))
                break
            // game start
            case 'gready':
                if(game==1){
                    gamestop=false
                    if(TimeTable()){
                        Command.broadcastCommandMessage(s,this.lang.translateString("command.gready.success"))
                    }else{
                        out=this.lang.translateString("command.gready.error")
                    }
                }else{
                    out=this.lang.translateString("command.gready.error")
                }
                break
            // force stop the game
            case 'gstop':
                if(gamestop){
                    // resume game
                    Command.broadcastCommandMessage(s,this.lang.translateString("command.gready.success"))
                    gamestop=false
                    if(Task.game.size()>=3){
                        Task.game.stream().map{it.taskId}.each{server.scheduler.cancelTask(it)}
                    }
                    if(game==10){
                        this.team.battleTeamMember.forEach{team,members->
                            members.forEach{member,number->
                                def player=server.getPlayer("$member")
                                if(player instanceof Player & player.hasEffect(Effect.BLINDNESS)){
                                    player.removeEffect(Effect.BLINDNESS)
                                }
                            }
                        }
                        def time=(int)((count_time-System.currentTimeMillis()+60)*20)
                        view.forEach{name,value->
                            def player=server.getPlayer("$name")
                            if(player instanceof Player){
                                player.removeEffect(Effect.JUMP)
                                player.removeEffect(Effect.FATIGUE)
                                // give effects again
                                player.addEffect(Effect.getEffect(Effect.JUMP).setDuration(time).setAmplifier(130).setVisible(false))
                                player.addEffect(Effect.getEffect(Effect.FATIGUE).setDuration(time).setAmplifier(5).setVisible(false))
                            }
                        }
                        startGame()
                    }else{
                        TimeTable()
                    }
                }else{
                    // stop game
                    Command.broadcastCommandMessage(s,this.lang.translateString("command.gstop.success"))
                    gamestop=true
                    if(game==10){
                        stopGame()
                        view.forEach{name,value->
                            def player=server.getPlayer("$name")
                            if(player instanceof Player){
                                Effect.getEffect(Effect.JUMP).setDuration(6000*20).setAmplifier(130).setVisible(false).add(player,true)
                                Effect.getEffect(Effect.FATIGUE).setDuration(6000*20).setAmplifier(5).setVisible(false).add(player,true)
                            }
                        }
                        this.team.battleTeamMember.forEach{team,members->
                            members.forEach{member,number->
                                def player=server.getPlayer("$member")
                                if(player instanceof Player){
                                    player.addEffect(Effect.getEffect(Effect.BLINDNESS).setDuration(6000*20).setAmplifier(0).setVisible(false))
                                }
                            }
                        }
                    }
                }
                break
            // force stop the game
            case 'gend':
                def msg=this.lang.translateString("command.gend.success")
                server.onlinePlayers.values().each {player->
                    def userx=player.name
                    def pt=(this.team.getBattleTeamOf(userx))?1000:15000
                    def playerData=Account.instance.getData(userx)
                    playerData.grantPoint(pt)
                    player.sendMessage("§3ゲームが途中で終了したため§e${pt}pt§3差し上げます")
                }
                if(game>=10){
                    if(gamestop){
                        out=this.lang.translateString("command.gend.error")
                    }else{
                        Command.broadcastCommandMessage(s,msg)
                        server.broadcastMessage("§3≫ $msg")
                        if(game==10){
                            unfinished=true
                            Task.game.end=server.scheduler.scheduleRepeatingTask(new GameEnd(this), 1)
                        }
                    }
                }else{
                    Command.broadcastCommandMessage(s,msg)
                    server.broadcastMessage("§3≫ $msg")
                    GameEnd()
                }
                break
            case 'gskip':
                if(!gamestop&TimeTable()){
                    Command.broadcastCommandMessage(s,this.lang.translateString("command.gskip.success"))
                    return true
                }else{
                    out=this.lang.translateString("command.gskip.error")
                }
                break
            // teleport everyone into respawn
            case 'tprall':
                server.onlinePlayers.values().each {tpr(it)}
                Command.broadcastCommandMessage(s,this.lang.translateString("commnad.tprall.success"))
                return true
            // force teleport to respawn
            // no params -> teleport yourself
            // 1 param -> teleport [the first param]
            case 'tpr':
                if(a.size()==1){
                    // teleporting others requires operator permission
                    if(s.op){
                        def player=server.getPlayer(a[0])
                        if(player){
                            tpr(player)
                            Command.broadcastCommandMessage(s,this.lang.translateString("command.tpr.success.admin",player.displayName))
                        }else{
                            out=this.lang.translateString("command.playerNotFound")
                        }
                    }else{
                        out=this.lang.translateString("command.notPermission")
                    }
                }else{
                    if(s instanceof Player){
                        out=this.lang.translateString("tpr.${tpr(s)?'respawn':'gameStartPoint'}")
                    }else{
                        return false
                    }
                }
                break
            // Main.php:2011
        }
        return true
    }
}
