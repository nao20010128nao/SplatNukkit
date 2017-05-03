package SplatoonMCPE

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.lang.BaseLang
import cn.nukkit.plugin.Plugin
import cn.nukkit.plugin.PluginBase

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
    public reconData = []
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
    public view = []
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
        db=new DataBase()
        a=new Accont(this)
        s=new StatusUnit(this)
        lang=new BaseLang(new File(dataFolder,'lang').absolutePath,s.language)
        w=new Weapon(this)
        team=new Team(this)
        entry=new Entry(this)

        team.init()
        data=true
        itemSelect=new ItemSelect(this,w.weaponsDataAll,lang)
        itemCase=new ItemCase(this)
        seat=new Seat()
        Task.tips=server.scheduler.scheduleRepeatingTask(new RandomTask(this),20*75)
        s.loginRestriction
        s.resetOnlineStat()
        creativeItemDelete()
        w.weaponsDataAllIntoDB
        updateStage()
        shop=new ItemShop(this,w.weaponsDataAll)

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
            a.saveAll(true)

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
            s.offline
            s.removeAllFromOnlineStat()
        }
    }

    /**
     * Set something about battle field
     * @param first true if the server is launching, false otherwise
     */
    void setData(boolean first=false){
        battleField=[
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
        w.weaponsName=weaponsName

        // Main.php:1324
    }
}
