import java.util.ArrayList;
import java.util.Map;

import com.sk89q.craftbook.HistoryHashMap;
import com.sk89q.craftbook.music.IMusicPlayer;
import com.sk89q.craftbook.music.MusicNote;
import com.sk89q.craftbook.music.Playlist;
import com.sk89q.craftbook.music.RadioObject;
import com.sk89q.craftbook.music.media.ExternalMedia;
import com.sk89q.craftbook.music.media.Media;
import com.sk89q.craftbook.music.media.MidiMedia;
import com.sk89q.craftbook.music.media.SimpleTuneMedia;
import com.sk89q.craftbook.music.media.TextSongMedia;



public class MusicPlayer implements IMusicPlayer
{
	private final boolean LOOP;
	private final int MAX_BEAT_DURATION;
	private final int MAX_TEXT_LINES;
	private final int MAX_PLAYLIST_TRACKS;
	private final int MAX_MIDI_TRACKS;
	private final int MAX_RATE = 10;
	private final int MAX_MISSING = 10;
	private final String NOW_PLAYING;
	
	private final int x;
	private final int y;
	private final int z;
	private final int worldType;
	
	private int currentTick = 0;
	private int skipProtection = 0;
	
	public boolean pause = false;
	
	private Media playingMedia;
	private Playlist playlist;
	
	private Map<String,RadioObject> radios;
	
	public MusicPlayer(String data, int worldType, int x, int y, int z, PropertiesFile properties, byte type, boolean loop)
	{
		this(data, worldType, x, y, z, properties, type, loop, false);
	}
	
	public MusicPlayer(String data, int worldType, int x, int y, int z, PropertiesFile properties, byte type, boolean loop, boolean isStation)
	{
		this.LOOP = loop;
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.worldType = worldType;
		
		if(isStation)
			radios = new HistoryHashMap<String,RadioObject>(100);
		else
			radios = null;
		
		int duration = 500;
		int linemax = 100;
		int playlistMax = 100;
		int trackmax = 10;
		String nowPlaying = "";
		
		if(properties != null)
		{
			if(properties.containsKey("music-max-beat-duration"))
			{
				duration = properties.getInt("music-max-beat-duration", duration);
				if(duration < 0)
					duration = 0;
			}
			
			if(properties.containsKey("music-max-text-lines"))
			{
				linemax = properties.getInt("music-max-text-lines", linemax);
				if(linemax < 0)
					linemax = 0;
			}
			
			if(properties.containsKey("music-max-playlist-tracks"))
			{
				playlistMax = properties.getInt("music-max-playlist-tracks", playlistMax);
				if(playlistMax < 0)
					playlistMax = 0;
			}
			
			if(properties.containsKey("music-max-midi-tracks"))
			{
				trackmax = properties.getInt("music-max-midi-tracks", trackmax);
				if(trackmax < 0)
					trackmax = 0;
			}
			if(isStation)
			{
				nowPlaying = "now playing";
				if(properties.containsKey("music-text-now-playing"))
				{
					nowPlaying = properties.getString("music-text-now-playing", nowPlaying);
				}
			}
		}
		else
		{
			type = 1;
		}
		
		MAX_BEAT_DURATION = duration;
		MAX_TEXT_LINES = linemax;
		MAX_PLAYLIST_TRACKS = playlistMax;
		MAX_MIDI_TRACKS = trackmax;
		NOW_PLAYING = nowPlaying;
		
		if(type == 0)
		{
			//external media
			playingMedia = parseExternalData(data, true);
			if(playingMedia == null && playlist != null)
			{
				playingMedia = playlist.getCurrentMedia();
			}
		}
		else
		{
			playingMedia = new SimpleTuneMedia(this, data);
		}
	}
	
	public ExternalMedia parseExternalData(String data)
	{
		return parseExternalData(data, false);
	}
	
	private ExternalMedia parseExternalData(String data, boolean includePlaylist)
	{
		String[] args = data.split(":", 2);
		String[] song = args[0].split("\\.", 2);
		
		ExternalMedia media = null;
		
		if(song.length > 1)
		{
			if(song[1].equalsIgnoreCase("m"))
				media = new MidiMedia(this, song[0]);
			else if(includePlaylist && song[1].equalsIgnoreCase("p"))
			{
				playlist = new Playlist(this, song[0]);
				if(playlist.getSize() == 0)
				{
					playlist = null;
				}
				media = null;
			}
		}
		else
		{
			media = new TextSongMedia(this, song[0]);
		}
		
		if(media == null)
			return null;
		
		int rate = -1;
		if(args.length > 1)
		{
			rate = getLimitedRate(args[1]);
		}
		
		media.setRate(rate);
		
		currentTick = media.getRate();
		
		return media;
	}
	
	public void tick()
	{
		if(!isPlaying())
			return;
		
		if(currentTick >= playingMedia.getRate())
		{
			currentTick = 0;
			if(!playingMedia.playNextNote())
			{
				if(playlist != null)
				{
					playNext();
				}
				else if(LOOP)
				{
					playingMedia.reset();
					sendInfoToRadios();
				}
				else
				{
					stop();
				}
				return;
			}
		}
		else
		{
			currentTick++;
		}
	}
	
	public void playNotes(ArrayList<MusicNote> notes)
	{
		for(MusicNote note : notes)
		{
			etc.getMCServer().f.a(x, y, z, 64.0D, worldType, new OPacket54PlayNoteBlock(x, y, z, note.type, note.pitch));
			
			if(radios != null)
			{
				for(RadioObject radio : radios.values())
				{
					etc.getMCServer().f.a(radio.X, radio.Y, radio.Z, 64.0D, worldType,
							new OPacket54PlayNoteBlock(radio.X, radio.Y, radio.Z, note.type, note.pitch));
				}
			}
		}
	}
	
	public void sendMessage(String message)
	{
		sendMessageTo(message, x, y, z);
	}
	
	private void sendMessageTo(String message, int x, int y, int z)
	{
		World world = CraftBook.getWorld(worldType);
		World.Type type = world.getType();
		for(Player player: etc.getServer().getPlayerList())
		{
			if(player.getWorld().getType() != type)
				continue;
			
			Location pLoc = player.getLocation();
			double diffX = x - pLoc.x;
			double diffY = y - pLoc.y;
			double diffZ = z - pLoc.z;
			
			if(diffX * diffX + diffY * diffY + diffZ * diffZ < 4096.0D)
			{
				String[] lines = message.split("<br>", 5);
				for(String line : lines)
				{
					player.sendMessage(line);
				}
			}
		}
	}
	
	private void sendInfoToRadios()
	{
		if(radios == null || playingMedia == null || !(playingMedia instanceof ExternalMedia))
			return;
		
		ExternalMedia media = (ExternalMedia) playingMedia;
		String message = media.getInfoMessage();
		
		for(RadioObject radio : radios.values())
		{
			if(radio.sendMessages || media.isForcedMessage())
				sendMessageTo(message, radio.X, radio.Y, radio.Z);
		}
	}
	
	public void playNext()
	{
		if(playlist == null)
			return;
		
		if(skipProtection > MAX_MISSING)
		{
			turnOff();
			return;
		}
		
		if(playingMedia != null)
			playingMedia.reset();
		
		playingMedia = playlist.getNext();
		if(playingMedia == null)
		{
			if(LOOP)
				playingMedia = playlist.jumpTo(0);
			
			if(playingMedia == null)
			{
				turnOff();
				return;
			}
		}
		
		if(playingMedia instanceof ExternalMedia)
		{
			if( !((ExternalMedia)playingMedia).loadSong())
			{
				skipProtection++;
				playNext();
				return;
			}
			skipProtection = 0;
			sendInfoToRadios();
		}
	}
	
	public void playPrevious()
	{
		if(playlist == null)
			return;
		
		if(skipProtection > MAX_MISSING)
		{
			turnOff();
			return;
		}
		
		if(playingMedia != null)
			playingMedia.reset();
		
		playingMedia = playlist.getPrevious();
		if(playingMedia == null)
		{
			if(LOOP)
				playingMedia = playlist.jumpTo(playlist.getSize()-1);
			
			if(playingMedia == null)
			{
				turnOff();
				return;
			}
		}
		
		if(playingMedia instanceof ExternalMedia)
		{
			if( !((ExternalMedia)playingMedia).loadSong())
			{
				skipProtection++;
				playPrevious();
				return;
			}
			skipProtection = 0;
			sendInfoToRadios();
		}
	}
	
	//this is really "pause"
	public void stop()
	{
		pause = true;
	}
	
	public void turnOff()
	{
		stop();
		
		if(radios != null)
		{
			World world = CraftBook.getWorld(worldType);
			for(RadioObject radio : radios.values())
			{
				ComplexBlock block = world.getComplexBlock(radio.SIGN_X, radio.SIGN_Y, radio.SIGN_Z);
		    	if(!(block instanceof Sign))
		    		return;
		    	
		    	Sign sign = (Sign) block;
		    	String title = MCX702.getOffState(sign.getText(0));
		    	sign.setText(0, title);
			}
			radios.clear();
		}
		
		playingMedia = null;
		playlist = null;
	}
	
	public void loadSong()
	{
		if(playingMedia == null || !(playingMedia instanceof ExternalMedia))
			return;
		
		ExternalMedia media = (ExternalMedia) playingMedia;
		if(!media.loadSong())
		{
			byte error;
			if(media instanceof MidiMedia && (error = ((MidiMedia) media).getErrorType()) != 0)
			{
				if(error == 1)
					sendMessage(Colors.Rose+"> Unsupported MIDI");
				else if(error == 2)
					sendMessage(Colors.Rose+"> Error reading MIDI");
			}
			return;
		}
		
		//sendInfoToRadios();
	}
	
	public boolean isPlaying()
	{
		return !pause && playingMedia != null && !playingMedia.isFinished();
	}
	
	public void addRadio(String key, RadioObject radio)
	{
		if(radios == null || radio == null)
			return;
		
		radios.put(key, radio);
		
		if(isPlaying() && playingMedia instanceof ExternalMedia)
		{
			ExternalMedia media = (ExternalMedia) playingMedia;
			
			if(radio.sendMessages || media.isForcedMessage())
				sendMessageTo(media.getInfoMessage(), radio.X, radio.Y, radio.Z);
		}
	}
	
	public RadioObject getRadio(String key)
	{
		if(radios == null)
			return null;
		
		return radios.get(key);
	}
	
	public void removeRadio(String key)
	{
		if(radios == null)
			return;
		
		radios.remove(key);
	}
	
	public int getMaxRate()
	{
		return MAX_RATE;
	}
	
	public int getLimitedRate(String sRate)
	{
		int rate = -1;
		try
		{
			rate = Integer.parseInt(sRate);
			if(rate > MAX_RATE)
				rate = MAX_RATE;
			else if(rate < 1)
				rate = -1;
		}
		catch(NumberFormatException e)
		{
			rate = -1;
		}
		
		return rate;
	}
	
	public int getMaxMidiTracks()
	{
		return MAX_MIDI_TRACKS;
	}
	
	public int getMaxBeatDuration()
	{
		return MAX_BEAT_DURATION;
	}
	
	public int getMaxTextLines()
	{
		return MAX_TEXT_LINES;
	}
	
	public int getMaxPlaylistTracks()
	{
		return MAX_PLAYLIST_TRACKS;
	}
	
	public String getNowPlaying()
	{
		return NOW_PLAYING;
	}
	
	public boolean loops()
	{
		return LOOP;
	}
}
