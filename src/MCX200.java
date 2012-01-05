// $Id$
/*
 * CraftBook
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
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

import com.sk89q.craftbook.*;
import com.sk89q.craftbook.ic.*;

/**
 * Mob spawner.
 *
 * @author sk89q
 */
public class MCX200 extends BaseIC {
	
	private final String TITLE = "MOB";
	protected String settings = "";
    /**
     * Get the title of the IC.
     *
     * @return
     */
    public String getTitle() {
        return settings;
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
    	String settings[] = sign.getLine1().split(":",2);
        String id = sign.getLine3();
        String rider = sign.getLine4();
        
        this.settings = TITLE;
        
        if(!settings[0].isEmpty())
        {
        	try
        	{
        		int amount = Integer.parseInt(settings[0]);
        		if(amount < 0 || amount > 9)
        			return "Amount must be a number from 1 to 9, or 0 for 10";
        		
        		this.settings += ":"+amount;
        		if(settings.length == 2 && sign.getLine2().contains("[MCU200]"))
        		{
        			int ticks = Integer.parseInt(settings[1]);
        			if(ticks < 5 || ticks > 999)
        				return "Rate must be a number from 5 to 999";
        			
        			this.settings = "^"+this.settings+":"+ticks+":0";
        		}
        	}
        	catch(NumberFormatException e)
        	{
        		return "Line 1 must contain numbers split with :";
        	}
        }
        
        if (id.length() == 0) {
            return "Specify a mob type on the third line.";
        }
        
        String[] args = id.split(":", 2);
        int color = getColor(args);
        
        if(color >= 0)
        	id = args[0];
        else if(color == -2)
        	return "Not a valid color value: " + args[1] + ".";
        
        String[] args2 = rider.split(":", 2);
        int colorRider = getColor(args2);
        if(colorRider >= 0)
        	rider = args2[0];
        else if(colorRider == -2)
        	return "Not a valid color value: " + args2[1] + ".";
        
        if (!Mob.isValid(id)) {
            return "Not a valid mob type: " + id + ".";
        } else if (rider.length() != 0 && !Mob.isValid(rider)) {
            return "Not a valid rider type: " + rider + ".";
        }

        return null;
    }

    /**
     * Think.
     *
     * @param chip
     */
    public void think(ChipState chip)
    {
    	if(chip.inputAmount() == 0)
    	{
    		String[] args = chip.getText().getLine1().split(":", 4);
    		if(args.length == 4)
    		{
    			int ticks = Integer.parseInt(args[2]);
    			int curTick = Integer.parseInt(args[3]);
    			
    			boolean spn = false;
    			curTick++;
    			if(curTick >= ticks)
    			{
    				spn = true;
    				curTick = 0;
    			}
    			
    			chip.getText().setLine1(args[0]+":"+args[1]+":"+args[2]+":"+curTick);
    			chip.getText().supressUpdate();
    			
    			if(spn)
    			{
    				spawnThink(chip);
    			}
    		}
    		else
    			return;
    	}
    	else
    	{
    		if(chip.getText().getLine1().charAt(0) == '^' || chip.getText().getLine1().charAt(0) == '%')
    		{
    			if(chip.getIn(1).isTriggered())
    			{
    				if(chip.getIn(1).is() && chip.getText().getLine1().charAt(0) == '^')
    				{
    					chip.getText().setLine1("%"+chip.getText().getLine1().substring(1));
    					chip.getText().supressUpdate();
    					
    					RedstoneListener listener = (RedstoneListener) chip.getExtra();
    					listener.onSignAdded(CraftBook.getWorld(chip.getWorldType()), chip.getPosition().getBlockX(), chip.getPosition().getBlockY(), chip.getPosition().getBlockZ());
    				}
    				else if(!chip.getIn(1).is() && chip.getText().getLine1().charAt(0) == '%')
    				{
    					String[] args = chip.getText().getLine1().split(":", 4);
    					chip.getText().setLine1("^"+args[0].substring(1)+":"+args[1]+":"+args[2]+":0");
    					chip.getText().supressUpdate();
    				}
    			}
    		}
    		else if(chip.getIn(1).is())
    		{
    			spawnThink(chip);
    		}
    	}
    }
    
    private void spawnThink(ChipState chip)
    {
    	String[] args = chip.getText().getLine1().split(":", 4);
    	
    	int amount = 1;
    	if(args.length > 1)
    	{
    		amount = Integer.parseInt(args[1]);
    	}
    	
    	String id = chip.getText().getLine3();
        String rider = chip.getText().getLine4();
        
        args = id.split(":", 2);
        int color = getColor(args);
        
        if(color >= 0)
        	id = args[0];
        
        String[] args2 = rider.split(":", 2);
        int colorRider = getColor(args2);
        if(colorRider >= 0)
        	rider = args2[0];
        
        if (Mob.isValid(id)) {
            Vector pos = chip.getBlockPosition();
            int maxY = Math.min(128, pos.getBlockY() + 10);
            int x = pos.getBlockX();
            int z = pos.getBlockZ();

            for (int y = pos.getBlockY() + 1; y <= maxY; y++)
            {
            	int blockId = CraftBook.getBlockID(chip.getWorldType(), x, y, z);
                if (BlockType.canPassThrough(blockId) || BlockType.isWater(blockId))
                {
                    Location loc = new Location(x, y, z);
                    loc.dimension = chip.getWorldType();
                    
                    for(int i = 0; i < amount; i++)
                    {
                        Mob mob = new Mob(id, loc);
                        if (rider.length() != 0 && Mob.isValid(rider)) {
                        	Mob mobRider = new Mob(rider);
                            //mob.spawn(mobRider);
                            spawn(mob, chip.getWorldType(), mobRider); //[TODO]: remove when Canary fixes Mob spawn's world
                            
                            if(colorRider >= 0)
                            	setMobColor(mobRider.getEntity(), colorRider);
                            
                        } else {
                            //mob.spawn();
                            spawn(mob, chip.getWorldType(), null); //[TODO]: remove when Canary fixes Mob spawn's world
                        }
                        
                        if(color >= 0)
                        	setMobColor(mob.getEntity(), color);
                    }
                    
                    return;
                }
            }
        }
    }
    
    //[TODO]: remove when Canary fixes Mob spawn's world
    private void spawn(BaseEntity entity, int worldType, BaseEntity rider)
    {
    	OWorld oworld = CraftBook.getOWorld(worldType);
    	OEntity oentity = entity.getEntity();
    	
    	oentity.c(entity.getX() + 0.5D, entity.getY(), entity.getZ() + 0.5D, entity.getRotation(), 0.0F);
    	oworld.b(oentity);
    	
    	if (rider != null)
    	{
    		OEntity orider = rider.getEntity();
    		orider.c(entity.getX(), entity.getY(), entity.getZ(), entity.getRotation(), 0.0F);
    		oworld.b(orider);
    		orider.b(oentity);
    	}
    }
    
    private int getColor(String[] args)
    {
    	int color;
    	
    	if(args.length < 2 || !isValidColorMob(args[0]) )
    		return -1;
    	
    	try
    	{
    		color = Integer.parseInt(args[1]);
    	}
    	catch(NumberFormatException e)
    	{
    		return -2;
    	}
    	
    	if(color < 0 || color > 15)
    		return -2;
    	
    	return color;
    }
    
    private void setMobColor(OEntityLiving entity, int color)
	{
    	if(entity instanceof OEntitySheep)
    	{
    		OEntitySheep sheep = (OEntitySheep)entity;
    		sheep.b(color);
    	}
    	else if(entity instanceof OEntityCreeper)
    	{
    		//no real need for this check, but putting it here to be 
    		//strict so that it's easier to support more creeper types
    		//if more are ever created
    		if(color != 1)
    			return;
    		
    		OEntityCreeper creeper = (OEntityCreeper)entity;
    		creeper.au().b(17, (byte)1);
    	}
    	else if(entity instanceof OEntityWolf)
    	{
    		//since a tamed wolf requires a player, I'm just allowing
    		//the option to create angry wolves and sitting wolves.
    		//neutral wolves have no color value (or are technically 0)
    		if(color != 2 && color != 1)
    			return;
    		
    		OEntityWolf wolf = (OEntityWolf)entity;
    		wolf.au().b(16, (byte)color);
    	}
    	else if(entity instanceof OEntityPig)
    	{
    		if(color != 1)
    			return;
    		
    		OEntityPig pig = (OEntityPig)entity;
    		pig.a(true);
    	}
	}
    
    private boolean isValidColorMob(String mob)
    {
    	if( mob.equals("Sheep")
    		|| mob.equals("Creeper")
    		|| mob.equals("Wolf")
    		|| mob.equals("Pig")
    		)
    	{
    		return true;
    	}
    	
    	return false;
    }
}
