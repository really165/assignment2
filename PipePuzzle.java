package assignment2;

public class PipePuzzle {
    //position in puzzle
    int x, y;
    //current puzzle
    char[][] current;    
    //booleans to keep track of which ways can be moved
    boolean up, down, left, right;
    
    
    public PipePuzzle(char[][] input){
        this.current = input;
    }
    
    public void setPosition(int xPos, int yPos){
        this.x = xPos;
        this.y = yPos;
    }
    
    public void setAvailableMoves(){
        //finds available moves
        if(x-1>=0){
            if(current[(x-1)][y]=='_'){
                up = true;
            }
        }
        if(x+1 < current.length){
            if(current[(x+1)][y]=='_'){
                down = true;
            }
        }
        if(y-1>=0){
            if(current[x][(y-1)]=='_'){
                left = true;
            }
        }
        if(y+1 < current[0].length){
            if(current[x][(y+1)]=='_'){
                right = true;
            }
        }
    }
}
