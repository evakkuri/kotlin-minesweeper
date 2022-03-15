package minesweeper

import kotlin.random.Random

typealias BoardLoc = Pair<Int, Int>

enum class GameStatus(val message: String) {
    OPEN(""), WON("Congratulations! You found all the mines!"), LOST("You stepped on a mine and failed!")
}

enum class Marker(val marker: String) {
    UNEXPLORED("."), MARKED("*"), EMPTY("/"), MINE("X")
}

class BoardSquare(
    var minesAround: Int = 0,
    var isMarked: Boolean = false,
    var isMine: Boolean = false,
    var isExplored: Boolean = false
) {
    override fun toString(): String {
        return when {
            this.isMarked && !this.isExplored -> Marker.MARKED.marker
            !this.isExplored -> Marker.UNEXPLORED.marker
            this.minesAround > 0 -> this.minesAround.toString()
            this.isMine -> Marker.MINE.marker
            else -> Marker.EMPTY.marker
        }
    }

    fun switchMark(): Int {
        val change = if (this.isMarked) -1 else 1
        this.isMarked = !this.isMarked
        return change
    }

    fun openAndIsMine(): Triple<Boolean, Boolean, Boolean> {
        var boardChanged = false

        if (this.isExplored && this.minesAround > 0) {
            println("There is a number there!")
        }

        else if (!this.isExplored) {
            this.isExplored = true
            this.isMarked = false
            boardChanged = true
        }

        return Triple(boardChanged, this.minesAround > 0, this.isMine)
    }
}

class Board(numMines: Int, private val sideLength: Int = 9) {
    private val board: MutableList<MutableList<BoardSquare>> = mutableListOf()
    private val mines = mutableListOf<Pair<Int, Int>>()
    private var numMarked = 0
    var status = GameStatus.OPEN

    init {
        // Add empty squares
        for (i in 0 until sideLength) {
            val boardRow = mutableListOf<BoardSquare>()
            for (j in 0 until sideLength) {
                boardRow.add(BoardSquare())
            }
            board.add(boardRow)
        }

        // Add mines
        while (mines.size < numMines) {
            val mineLocation = Pair(Random.nextInt(0, sideLength), Random.nextInt(0, sideLength))
            if (mineLocation !in mines) mines.add(mineLocation)
            board[mineLocation.first][mineLocation.second].isMine = true
        }

        for (mine in mines) {
            println("Setting square ${mine.second + 1}, ${mine.first + 1} as mine")

            for (row in mine.first - 1..mine.first + 1) {
                for (col in mine.second - 1..mine.second + 1) {
                    if (row !in 0 until sideLength
                        || col !in 0 until sideLength
                        || board[row][col].isMine) {
                        continue
                    }

                    board[row][col].minesAround += 1
                }
            }
        }
    }

    fun updateStatus(): GameStatus {
        if (this.mines.all { mineSquare -> board[mineSquare.first][mineSquare.second].isMarked }
            && this.numMarked == this.mines.size) this.status = GameStatus.WON
        else if (this.board.all { boardRow ->
                boardRow.filter { square -> !square.isMine }.all { square -> square.isExplored }
            }) this.status = GameStatus.WON

        return this.status
    }

    fun mark(row: Int, col: Int): Boolean {
        val change = this.board[row][col].switchMark()
        this.numMarked += change
        return change != 0
    }

    fun open(row: Int, col: Int): Boolean {
        val squaresToOpen = ArrayDeque<BoardLoc>()
        var boardChanged = false

        squaresToOpen.add(BoardLoc(row, col))

        while (!squaresToOpen.isEmpty()) {
            val loc = squaresToOpen.removeFirst()

            // Open board square
            val (changed, foundMinesAround, openedMine) = this.board[loc.first][loc.second].openAndIsMine()
            if (!boardChanged && changed) boardChanged = true

            // If square contains a mine, return immediately
            if (openedMine) {
                this.status = GameStatus.LOST
                break
            }

            // If square has mines around it, continue to next one
            if (foundMinesAround) continue

            // Else open all conjoining squares until squares with mines around are met
            for (rowAdjacent in loc.first-1..loc.first+1 ) {
                for (colAdjacent in loc.second-1..loc.second+1) {
                    if (rowAdjacent !in 0 until this.sideLength
                        || colAdjacent !in 0 until this.sideLength
                        || this.board[rowAdjacent][colAdjacent].isExplored
                        || BoardLoc(rowAdjacent, colAdjacent) in squaresToOpen
                    ) continue

                    else squaresToOpen.add(BoardLoc(rowAdjacent, colAdjacent))
                }
            }
        }

        return boardChanged
    }

    fun print() {
        println(" |${(1..this.sideLength).joinToString("")}|")
        println("-|${"-".repeat(this.sideLength)}|")
        this.board.forEachIndexed { i, row ->
            println("${i + 1}|${row.joinToString("")}|")
        }
        println("-|${"-".repeat(this.sideLength)}|")
    }
}

fun main() {
    print("How many mines do you want on the field? ")
    val numMines = readln().toInt()
    val board = Board(numMines)
    board.print()

    while (board.status == GameStatus.OPEN) {
        print("Set/delete mines marks (x and y coordinates): ")
        val (colStr, rowStr, command) = readln().split(" ")
        val row = rowStr.toInt() - 1
        val col = colStr.toInt() - 1

        val boardChanged = when(command) {
            "mine" -> board.mark(row, col)
            "free" -> board.open(row, col)
            else -> {
                println("Unknown command $command")
                false
            }
        }

        if (boardChanged) {
            board.print()
        }

        board.updateStatus()
    }

    println(board.status.message)
}
