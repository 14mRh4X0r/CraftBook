// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Iterator;

import com.sk89q.craftbook.*;
import com.sk89q.craftbook.ic.*;


public class MCX140 extends BaseIC {
	
    /**
     * Get the title of the IC.
     *
     * @return
     */
	private final String TITLE = "IN AREA";
	
    public String getTitle() {
    	return "^"+TITLE;
    }

    /**
     * Returns true if this IC requires permission to use.
     *
     * @return
     */
    public boolean requiresPermission() {
        return true;
    }

    /**
     * Validates the IC's environment. The position of the sign is given.
     * Return a string in order to state an error message and deny
     * creation, otherwise return null to allow.
     *
     * @param sign
     * @return
     */
    public String validateEnvironment(int worldType, Vector pos, SignText sign) {
    	
    	if(sign.getLine3().isEmpty())
    	{
    		return "3rd line must contain an entity name/type";
    	}
    	else
    	{
    		String[] args = sign.getLine3().split("\\+", 2);
    		if(!isValidEntityName(args[0]))
    		{
    			return "Invalid name on Line 3";
    		}
    		
    		if(args.length > 1 && !isValidEntityName(args[1]))
    		{
    			return "Invalid rider name on Line 3";
    		}
    	}
    	
    	if(!sign.getLine4().isEmpty())
    	{
    		isValidDimensions(sign.getLine4());
    	}
    	
        return null;
    }
    
    protected static String isValidDimensions(String settings)
    {
    	String[] args = settings.split("/", 2);
		String[] dim = args[0].split(":", 3);
		if(dim.length != 3)
			return "4th line format: width:height:length/x-offset:y-offset:z-offset";
		try
		{
			int width = Integer.parseInt(dim[0]);
			int height = Integer.parseInt(dim[1]);
			int length = Integer.parseInt(dim[2]);
			if(width < 1 || width > 16 || height < 1 || height > 16 || length < 1 || length > 16)
				return "width, height, and length must be a number from 1 to 16";
			
			if(args.length > 1)
			{
				String[] offsets = args[1].split(":", 3);
				if(offsets.length != 3)
					return "4th line format: width:height:length/x-offset:y-offset:z-offset";
				
				int offx = Integer.parseInt(offsets[0]);
				int offy = Integer.parseInt(offsets[1]);
				int offz = Integer.parseInt(offsets[2]);
				
				if(offx < -10 || offx > 10
					|| offy < -10 || offy > 10
					|| offz < -10 || offz > 10)
				{
					return "offset values must be a number from -10 to 10";
				}
			}
		}
		catch(NumberFormatException e)
		{
			return "4th line format: width:height:length/x-offset:y-offset:z-offset";
		}
		
		return null;
    }
    
    protected static boolean isValidEntityName(String entityName)
    {
    	entityName = entityName.split(":", 2)[0];
    	if(entityName.isEmpty())
    		return false;
    	
    	entityName = entityName.toUpperCase();
    	
    	return entityName.equals("P") || entityName.equals("PLAYER") || entityName.equals("PLY")
    			|| entityName.equals("G") || entityName.equals("GROUP") || entityName.equals("GRP")
    			|| entityName.equals("MOB") || entityName.equals("ANIMAL")
    			|| Mob.isValid(entityName)
    			|| entityName.equals("MINECART")
    			|| entityName.equals("BOAT")
    			|| entityName.equals("ITEM")
    			|| entityName.equals("ARROW") || entityName.equals("EGG") || entityName.equals("SNOWBALL") || entityName.equals("FIREBALL")
    			;
    }
    
    protected static boolean isValidEntity(BaseEntity entity, String args)
    {
    	if(entity == null)
    		return false;
    	
    	String[] values = args.split(":", 2);
    	String entityName = values[0];
    	
    	if(entityName.equals("P") || entityName.equals("PLAYER") || entityName.equals("PLY"))
    	{
    		if(values.length > 1)
    		{
    			return entity.getName().equalsIgnoreCase(values[1]);
    		}
    		
    		return entity.getEntity() instanceof OEntityPlayerMP;
    	}
    	else if(entityName.equals("G") || entityName.equals("GROUP") || entityName.equals("GRP"))
    	{
    		if(values.length > 1 && entity.getEntity() instanceof OEntityPlayerMP)
    		{
    			Player player = new Player((OEntityPlayerMP)entity.getEntity());
    			player.isInGroup(values[1]);
    		}
    		
    		return false;
    	}
    	else if(entityName.equals("MOB"))
    	{
    		return entity.isMob();
    	}
		else if(entityName.equals("ANIMAL"))
		{
			return entity.isAnimal();
		}
		else if(Mob.isValid(entityName))
		{
			if(values.length > 1 && !entity.isPlayer())
			{
				return entity.getName().equalsIgnoreCase(values[1]);
			}
			return false;
		}
		else if(entityName.equals("MINECART"))
		{
			return entity.getEntity() instanceof OEntityMinecart;
		}
		else if(entityName.equals("BOAT"))
		{
			return entity.getEntity() instanceof OEntityBoat;
		}
		else if(entityName.equals("ITEM"))
		{
			return entity.isItem();
		}
		else if(entityName.equals("ARROW"))
		{
			return entity.getEntity() instanceof OEntityArrow;
		}
		else if(entityName.equals("EGG"))
		{
			return entity.getEntity() instanceof OEntityEgg;
		}
		else if(entityName.equals("SNOWBALL"))
		{
			return entity.getEntity() instanceof OEntitySnowball;
		}
		else if(entityName.equals("FIREBALL"))
		{
			return entity.getEntity() instanceof OEntityFireball;
		}
    	
    	return false;
    }
    
    /**
     * Think.
     *
     * @param chip
     */
    public void think(ChipState chip)
    {
    	if(chip.inputAmount() == 0
    		|| (chip.getText().getLine2().charAt(3) == 'X' && chip.getIn(1).isTriggered() && chip.getIn(1).is())
    		)
    	{
	    	int width = 3;
	    	int height = 1;
	    	int length = 3;
	    	int offx = 0;
	    	int offy = 1;
	    	int offz = 0;
	    	
	    	if(!chip.getText().getLine4().isEmpty())
	    	{
	    		String[] args = chip.getText().getLine4().split("/", 2);
	    		String[] dim = args[0].split(":", 3);
	    		
	    		width = Integer.parseInt(dim[0]);
	    		height = Integer.parseInt(dim[1]);
	    		length = Integer.parseInt(dim[2]);
	    		
	    		if(args.length > 1)
	    		{
	    			String[] offsets = args[1].split(":", 3);
	    			offx = Integer.parseInt(offsets[0]);
	    			offy = Integer.parseInt(offsets[1]);
	    			offz = Integer.parseInt(offsets[2]);
	    		}
	    	}
	    	
	    	World world = CraftBook.getWorld(chip.getWorldType());
	    	Vector lever = Util.getWallSignBack(chip.getWorldType(), chip.getPosition(), 2);
	        int data = CraftBook.getBlockData(world, chip.getPosition());
	        BlockArea area = MCX220.getBlockArea(chip, data, width, height, length, offx, offy, offz);
	        
	        detectEntity(world, lever, area, chip);
    	}
    	else if(chip.getIn(1).isTriggered())
    	{
    		if(chip.getIn(1).is() && chip.getText().getLine1().charAt(0) != '%')
    		{
    			chip.getText().setLine1("%"+chip.getText().getLine1().substring(1));
    			chip.getText().supressUpdate();
    			
    			RedstoneListener redListener = (RedstoneListener) chip.getExtra();
    			redListener.onSignAdded(CraftBook.getWorld(chip.getWorldType()), chip.getPosition().getBlockX(), chip.getPosition().getBlockY(), chip.getPosition().getBlockZ());
    		}
    		else if(!chip.getIn(1).is() && chip.getText().getLine1().charAt(0) != '^')
    		{
    			chip.getText().setLine1("^"+chip.getText().getLine1().substring(1));
    			chip.getText().supressUpdate();
    		}
    	}
    }
    
    protected void detectEntity(World world, Vector lever, BlockArea area, ChipState chip)
    {
    	String[] args = chip.getText().getLine3().split("\\+", 2);
        
        DetectEntityInArea detectEntity = new DetectEntityInArea(area, lever, args[0], args.length > 1 ? args[1] : null, null, null);
        etc.getServer().addToServerQueue(detectEntity);
    }
    
    public class DetectEntityInArea implements Runnable
    {
    	private final BlockArea AREA;
    	private final Vector LEVER;
    	private final String ENTITY_NAME;
    	private final String RIDER_NAME;
    	private final Location DESTINATION;
    	private final String[] MESSAGES;
    	
    	public DetectEntityInArea(BlockArea area, Vector lever, String entityName, String riderName, Location destination, String[] messages)
    	{
    		AREA = area;
    		LEVER = lever;
    		ENTITY_NAME = entityName;
    		RIDER_NAME = riderName;
    		DESTINATION = destination;
    		MESSAGES = messages;
    	}
    	
		@Override
		public void run()
		{
			boolean output = false;
			
			OWorldServer oworld = CraftBook.getOWorldServer(AREA.getWorldType());
			for(@SuppressWarnings("rawtypes")
    		Iterator it = oworld.g.iterator(); it.hasNext();)
    		{
    			Object obj = it.next();
    			
    			if(!(obj instanceof OEntity))
    			{
    				//outdated?
    				return;
    			}
    			
    			BaseEntity entity = new BaseEntity((OEntity)obj);
    			
    			if(MCX140.isValidEntity(entity, ENTITY_NAME)
    				&& (RIDER_NAME == null || RIDER_NAME.isEmpty() || MCX140.isValidEntity(entity, RIDER_NAME))
    				&& AREA.containsPoint(AREA.getWorldType(),
    										OMathHelper.b(entity.getX()),
    										OMathHelper.b(entity.getY()),
    										OMathHelper.b(entity.getZ()) )
    				)
    			{
    				output = true;
    				
    				if(DESTINATION != null)
    				{
    					if(MESSAGES != null && entity.isPlayer())
    					{
    						Player player = new Player((OEntityPlayerMP)entity.getEntity());
    						for(String message : MESSAGES)
    	                	{
    	                		if(message == null)
    	                			break;
    	                		player.sendMessage(Colors.Gold+message);
    	                	}
    					}
    					
    					if(entity.isPlayer())
    					{
    						Player player = new Player((OEntityPlayerMP)entity.getEntity());
    						player.teleportTo(DESTINATION);
    					}
    					else
    					{
    						entity.teleportTo(DESTINATION);
    					}
    				}
    				
    				break;
    			}
    		}
			
			Redstone.setOutput(AREA.getWorldType(), LEVER, output);
		}
    }
}
